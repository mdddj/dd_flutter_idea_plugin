package shop.itbug.flutterx.model

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import shop.itbug.flutterx.common.yaml.DartYamlModel
import shop.itbug.flutterx.common.yaml.PubspecYamlFileTools
import shop.itbug.flutterx.services.PubService
import shop.itbug.flutterx.util.PubspecYamlElementFactory
import shop.itbug.flutterx.widget.MyFlutterPackage


sealed class PubPackageSearchState {
    object Loading : PubPackageSearchState()
    object Empty : PubPackageSearchState()
    data class Error(val error: String) : PubPackageSearchState()
    data class Result(val data: List<PubPackageInfo>) : PubPackageSearchState()
}


@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PubSearchViewModel(
    val viewModelScope: CoroutineScope,
    val pubService: PubService,
    val yamlFileTool: PubspecYamlFileTools,
    val elementFactory: PubspecYamlElementFactory
) {
    //    var outerSplitState by mutableStateOf(SplitLayoutState(0.5f))
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private var _pubPackageSearchState = MutableStateFlow<PubPackageSearchState>(PubPackageSearchState.Empty)
    val searchStateFlow = _pubPackageSearchState.asStateFlow()

    private val _allDeps = MutableStateFlow(emptyList<DartYamlModel>())
    val allDeps = _allDeps.asStateFlow()

    private val _guessTabSelectIndex = MutableStateFlow(0)
    val guessTabSelectIndex: StateFlow<Int> = _guessTabSelectIndex.asStateFlow()


    private val _packageGroupInfoModels = MutableStateFlow(mapOf<String, PubPackageInfo>())
     val packageGroupInfoModels = _packageGroupInfoModels.asStateFlow()

    private val _loading = MutableStateFlow(false)
     val loading = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300L)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isEmpty()) {
                        flowOf(PubPackageSearchState.Empty)
                    } else {
                        flow {
                            emit(PubPackageSearchState.Loading)
                            try {
                                val packages = pubService.search(query)?.packages ?: emptyList()
                                val result = pubService.findAllPluginInfo(packages.map { it.`package` })
                                emit(PubPackageSearchState.Result(result))
                            } catch (e: Exception) {
                                emit(PubPackageSearchState.Error(e.localizedMessage ?: "Unknown error"))
                            }
                        }.flowOn(Dispatchers.IO)
                    }
                }
                .collect { state ->
                    _pubPackageSearchState.value = state // 将最终结果更新到UI状态
                }
        }

        viewModelScope.launch {
            _allDeps.value = yamlFileTool.allDependencies()
        }
    }

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    //从文件中刷新依赖
    fun refreshAllDeps() {
        viewModelScope.launch {
            _allDeps.value = yamlFileTool.allDependencies()
        }
    }

    fun addDepToFile(model: PubPackageInfo, type: FlutterPluginType) {
        val file = yamlFileTool.file
        elementFactory.addDependencies(
            model.model.name,
            "^" + model.model.latest.version,
            file,
            type
        )
        refreshAllDeps()
    }

    fun changeTabIndex(index: Int) {
        _guessTabSelectIndex.value = index
    }


    suspend fun getAllPackageInfo(items: List<MyFlutterPackage>) {

        _loading.value = true
        fun fetchPackageInfo(name: String): Pair<String, PubPackageInfo> {
            val result = pubService.getPubPackageInfoModel(name)
            return Pair(name, result)
        }

        val uniquePackageNames = items.flatMap { myPackage ->
            when (myPackage) {
                is MyFlutterPackage.Simple -> listOf(myPackage.packageName)
                is MyFlutterPackage.Group -> myPackage.packages.map { it.name }
            }
        }.distinct()

        withContext(Dispatchers.IO){
            val deferredResults = uniquePackageNames.map { packageName ->
                async(Dispatchers.IO) {
                    try {
                        fetchPackageInfo(packageName)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            val result = deferredResults.awaitAll()
            val successPackages: Map<String, PubPackageInfo> = result.filterNotNull()
                .associate { it.first to it.second }
            _packageGroupInfoModels.value = successPackages
            _loading.value = false

        }
    }
}