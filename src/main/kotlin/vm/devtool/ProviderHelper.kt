package vm.devtool

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vm.VmService
import vm.element.Instance
import vm.element.InstanceKind
import vm.element.InstanceRef
import vm.element.MapAssociation

data class ProviderNode(
    val id: String,
    val type: String
)


/**
 * 表示属性的路径，用于跟踪展开状态。
 * 使用 data class 来获得 equals/hashCode 的自动实现。
 */
sealed class PathToProperty {
    data class ListIndex(val index: Int) : PathToProperty()
    data class MapKey(val ref: String) : PathToProperty()
    data class ObjectProperty(val name: String, val ownerUri: String, val ownerName: String) : PathToProperty()
}


/**
 * 表示从根对象到一个特定属性的完整路径。
 */
sealed class InstancePath {
    abstract val pathToProperty: List<PathToProperty>
    abstract fun copy(pathToProperty: List<PathToProperty>): InstancePath
    data class FromInstanceId(
        val instanceId: String,
        override val pathToProperty: List<PathToProperty> = emptyList()
    ) : InstancePath() {
        override fun copy(pathToProperty: List<PathToProperty>): InstancePath {
            return this.copy(instanceId = this.instanceId, pathToProperty = pathToProperty)
        }
    }

    data class FromProviderId(
        val providerId: String,
        override val pathToProperty: List<PathToProperty> = emptyList()
    ) : InstancePath() {
        override fun copy(pathToProperty: List<PathToProperty>): InstancePath {
            return this.copy(providerId = this.providerId, pathToProperty = pathToProperty)
        }
    }

    /**
     * 创建一个新的路径，该路径指向当前路径下的一个子属性。
     */
    fun pathForChild(property: PathToProperty): InstancePath {
        val newPath = pathToProperty + property
        return this.copy(pathToProperty = newPath)
    }

    fun pathForChildWithInstance(property: PathToProperty, instanceId: String): FromInstanceId {
        val newPath = pathToProperty + property
        return FromInstanceId(instanceId = instanceId, newPath)
    }


}


/**
 * 封装一个对象的字段信息。
 */
data class ObjectField(
    val name: String,
    val isFinal: Boolean,
    val ownerName: String,
    val ownerUri: String,
    val instanceId: String
)

/**
 * 封装不同类型实例的详细信息，用于UI展示。
 * 这是一个 sealed class，类似于 Dart 中的 freezed union type。
 */
sealed class InstanceDetails {
    data object Nill : InstanceDetails()
    data class Bool(val displayString: String, val instanceRefId: String) : InstanceDetails()
    data class Number(val displayString: String, val instanceRefId: String) : InstanceDetails()
    data class DartString(val displayString: String, val instanceRefId: String) : InstanceDetails()
    data class Enum(val type: String, val value: String, val instanceRefId: String) : InstanceDetails()

    data class DartList(val length: Int, val hash: Int, val instanceRefId: String, val elements: List<InstanceRef>) :
        InstanceDetails()

    data class Map(
        val associations: List<MapAssociation>, // 存储键值对的引用
        val hash: Int,
        val instanceRefId: String
    ) : InstanceDetails()

    data class Object(
        val type: String,
        val fields: List<ObjectField>, // 注意，这里的 List 来自 kotlin.collections
        val hash: Int,
        val instanceRefId: String,
        val evalForInstance: EvalOnDartLibrary // 每个对象实例都关联一个在其库上下文中执行的 eval
    ) : InstanceDetails()

    data class MapEntry(val key: InstanceDetails, val value: InstanceDetails)

    val isExpandable: Boolean
        get() = when (this) {
            is DartList -> length > 0
            is Map -> associations.isNotEmpty()
            is Object -> fields.isNotEmpty()
            else -> false
        }
}

object ProviderHelper {

    suspend fun getProviderNodes(vm: VmService): List<ProviderNode> {
        val mainIsolateId = vm.getMainIsolateId()
        val providerEval = EvalOnDartLibrary("package:provider/src/provider.dart", vm, vm.coroutineScope)
        val instanceRef = providerEval.safeEval(
            mainIsolateId,
            "ProviderBinding.debugInstance.providerDetails.keys.toList()"
        )
        val instance = providerEval.getInstance(mainIsolateId, instanceRef)
        val elements = instance.getElements() ?: return emptyList()
        suspend fun getNode(idRef: InstanceRef): ProviderNode? {
            val providerIdInstance = providerEval.getInstance(mainIsolateId, idRef)
            val id = providerIdInstance.getValueAsString() ?: return null
            val node = providerEval.safeEval(
                mainIsolateId,
                "ProviderBinding.debugInstance.providerDetails['${id}']"
            )
            val nodeInstance = providerEval.getInstance(mainIsolateId, node)
            val fields = nodeInstance.getFields()
            val typeField = fields?.first { it.getDecl()?.getName() == "type" }?.getValue()
            if (typeField != null) {
                val type = providerEval.getInstance(mainIsolateId, typeField).getValueAsString() ?: return null
                return ProviderNode(id, type)
            }
            return null
        }

        return withContext(Dispatchers.IO) {
            elements.map { getNode(it) }
        }.filterNotNull()
    }

    /**
     * 根据给定的路径获取一个实例的【单层】详细信息。
     * 它不再递归，只解析由 path 指定的那个对象。
     */
    suspend fun getInstanceDetails(vm: VmService, path: InstancePath): InstanceDetails {
        val mainIsolateId = vm.getMainIsolateId()
        val coreEval = EvalOnDartLibrary("dart:core", vm, vm.coroutineScope)

        val currentRef = when (path) {
            is InstancePath.FromProviderId -> {
                val providerEval = EvalOnDartLibrary("package:provider/src/provider.dart", vm, vm.coroutineScope)
                providerEval.safeEval(
                    mainIsolateId,
                    "ProviderBinding.debugInstance.providerDetails[\"${path.providerId}\"]?.value"
                )
            }

            is InstancePath.FromInstanceId -> {
                val dartEval = EvalOnDartLibrary("dart:io", vm, vm.coroutineScope)
                dartEval.safeEval(vm.getMainIsolateId(), "value", mapOf("value" to path.instanceId))
            }
        }
        val instance = coreEval.getInstance(mainIsolateId, currentRef)
        return instanceToDetails(instance, vm, mainIsolateId, path)
    }


    private suspend fun instanceToDetails(
        instance: Instance,
        vm: VmService,
        isolateId: String,
        path: InstancePath
    ): InstanceDetails {
        val coreEval = EvalOnDartLibrary("dart:core", vm, vm.coroutineScope)
        val instanceRefId = instance.getId()!!
        val hash = instance.getIdentityHashCode()

        return when (instance.getKind()) {
            InstanceKind.String -> InstanceDetails.DartString(instance.getValueAsString() ?: "", instanceRefId)
            InstanceKind.Bool -> InstanceDetails.Bool(instance.getValueAsString() ?: "false", instanceRefId)
            InstanceKind.Int, InstanceKind.Double -> InstanceDetails.Number(
                instance.getValueAsString() ?: "0",
                instanceRefId
            )

            InstanceKind.Null -> InstanceDetails.Nill
            InstanceKind.List -> InstanceDetails.DartList(
                length = instance.getLength(),
                hash = hash,
                instanceRefId = instanceRefId,
                elements = instance.getElements()?.toList() ?: emptyList()
            )

            InstanceKind.Map -> {
                val associations = instance.getAssociations()?.map { it } ?: emptyList()
                InstanceDetails.Map(associations, hash, instanceRefId)
            }

            else -> {

                if (path.pathToProperty.isNotEmpty()) {
                    println(path)
                    println(instance)
                }


                val classRef = instance.getClassRef()
                val libraryUri = classRef.getLibrary()?.getUri() ?: "dart:core"
                val evalForInstance = EvalOnDartLibrary(libraryUri, vm, vm.coroutineScope)
                val allFields = mutableListOf<ObjectField>()
                var currentClass = coreEval.getClassObject(isolateId, classRef.getId()!!)
                val evalCache = mutableMapOf<String, EvalOnDartLibrary>()
                evalCache[libraryUri] = evalForInstance
                while (currentClass != null) {
                    currentClass.getFields().forEach { fieldRef ->
                        try {
                            val fieldObj = coreEval.getFieldObject(isolateId, fieldRef.getId()!!) ?: return@forEach
                            val fieldName = fieldObj.getName()
                            val ownerClassRef = fieldObj.getOwner()
                            val ownerLibrary =
                                (coreEval.getObjectWithLibrary(isolateId, ownerClassRef.getLibrary()!!.getId()!!))
                                    ?: return@forEach
                            val ownerLibraryUri = ownerLibrary.getUri()!!

                            allFields.add(
                                ObjectField(
                                    name = fieldName,
                                    isFinal = fieldObj.isFinal(),

                                    ownerName = ownerClassRef.getName(),
                                    ownerUri = ownerLibraryUri,
                                    instanceId = fieldRef.getId()!!
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val superClassRef = currentClass.getSuperClass()
                    currentClass = if (superClassRef != null) {
                        coreEval.getClassObject(isolateId, superClassRef.getId()!!)
                    } else {
                        null
                    }
                }

                InstanceDetails.Object(
                    type = classRef.getName(),
                    fields = allFields.distinctBy { it.name }.sortedBy { it.name },
                    hash = instance.getIdentityHashCode(),
                    instanceRefId = instance.getId()!!,
                    evalForInstance = evalForInstance
                )
            }
        }
    }
}