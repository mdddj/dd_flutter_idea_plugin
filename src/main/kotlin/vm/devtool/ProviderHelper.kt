package vm.devtool

import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vm.VmService
import vm.element.*
import vm.getObject
import vm.getObjectWithClassObj
import vm.logging.Logging

data class ProviderNode(
    val id: String,
    val type: String
) {
    override fun toString(): String {
        return GsonBuilder().setPrettyPrinting().create().toJson(this)
    }

    fun getProviderPath(): InstancePath.FromProviderId {
        return InstancePath.FromProviderId(id)
    }
}


/**
 * 表示属性的路径，用于跟踪展开状态。
 * 使用 data class 来获得 equals/hashCode 的自动实现。
 */
sealed class PathToProperty {
    data class ListIndex(val index: Int) : PathToProperty()
    data class MapKey(val ref: String? = null) : PathToProperty()
    data class ObjectProperty(val name: String, val ownerUri: String, val ownerName: String, val field: ObjectField) :
        PathToProperty()
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


suspend fun ObjectField.getFieldInstance(vmService: VmService, parentInstance: InstanceDetails.Object): Instance? {
    if (isStatic) return null
    val eval: EvalOnDartLibrary = this.eval
    val expression = "(parent as ${this.ownerName}).${this.name}"
    val instanceRef = eval.safeEval(
        vmService.getMainIsolateId(),
        expression,
        mapOf("parent" to parentInstance.instanceRefId)
    )
    val instance = vmService.getObject(
        vmService.getMainIsolateId(),
        instanceRef.getId()!!
    )
    return instance
}

/**
 * 封装一个对象的字段信息。
 */
data class ObjectField(
    val name: String,
    val isFinal: Boolean,
    val ownerName: String,
    val ownerUri: String,
    val ref: InstanceRef? = null,
    @Transient
    val eval: EvalOnDartLibrary,
    val isDefinedByDependency: Boolean,
    val isStatic: Boolean,
) {
    override fun toString(): String {
        return GsonBuilder().setPrettyPrinting().create().toJson(this)
    }

    val isPrivate get() = name.startsWith("_")

    fun createEval(vm: VmService): EvalOnDartLibrary = EvalOnDartLibrary("dart:io", vm)
    fun createEvalWithOwner(vm: VmService): EvalOnDartLibrary = EvalOnDartLibrary(ownerUri, vm)

    suspend fun getInstance(vm: VmService, parentInstance: InstanceDetails.Object): Instance? {
        return this.getFieldInstance(vm, parentInstance)
    }
}

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
        @Transient
        val evalForInstance: EvalOnDartLibrary // 每个对象实例都关联一个在其库上下文中执行的 eval
    ) : InstanceDetails() {
        val fieldsFiltered get() = fields.filter { it.isDefinedByDependency.not() }.filter { it.isStatic.not() }
    }

    data class MapEntry(val key: InstanceDetails, val value: InstanceDetails)

    val isExpandable: Boolean
        get() = when (this) {
            is DartList -> length > 0
            is Map -> associations.isNotEmpty()
            is Object -> fields.isNotEmpty()
            else -> false
        }

    override fun toString(): String {
        return GsonBuilder().setPrettyPrinting().create().toJson(this)
    }
}

object ProviderHelper {

    suspend fun getProviderNodes(vm: VmService): List<ProviderNode> {
        val mainIsolateId = vm.getMainIsolateId()
        val providerEval = EvalOnDartLibrary("package:provider/src/provider.dart", vm)
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
    suspend fun getInstanceDetails(
        vm: VmService,
        path: InstancePath,
        parent: InstanceDetails? = null
    ): InstanceDetails {
        val mainIsolateId = vm.getMainIsolateId()
        val coreEval = EvalOnDartLibrary("dart:core", vm)
        val currentRef: InstanceRef? = if (parent == null) when (path) {
            is InstancePath.FromProviderId -> {
                val providerEval = EvalOnDartLibrary("package:provider/src/provider.dart", vm)
                providerEval.safeEval(
                    mainIsolateId,
                    "ProviderBinding.debugInstance.providerDetails[\"${path.providerId}\"]?.value"
                )
            }

            is InstancePath.FromInstanceId -> {
                Logging.getLogger().logInformation("获取实例详情")
                val dartEval = EvalOnDartLibrary("dart:io", vm)
                dartEval.safeEval(vm.getMainIsolateId(), "value", mapOf("value" to path.instanceId))
            }
        } else when (parent) {
            is InstanceDetails.Map -> {
                val keyPath = path.pathToProperty.last() as PathToProperty.MapKey
                val key = if (keyPath.ref == null) "null" else "key"
                val keyPathRef = keyPath.ref

                val scope = mutableMapOf("parent" to parent.instanceRefId)
                if (keyPathRef != null) {
                    scope["key"] = keyPathRef
                }
                coreEval.safeEval(mainIsolateId, "parent[$key]", scope)
            }

            is InstanceDetails.Bool -> {
                null
            }

            is InstanceDetails.Enum -> {
                null
            }

            is InstanceDetails.Nill -> {
                null
            }

            is InstanceDetails.Number -> {
                null
            }

            is InstanceDetails.Object -> {
                val propertyPath = path.pathToProperty.last() as PathToProperty.ObjectProperty
                val field = parent.fields.first {
                    it.name == propertyPath.name
                            && it.ownerName == propertyPath.ownerName && it.ownerUri == propertyPath.ownerUri
                }
                Logging.getLogger().logInformation("获取字段详情")
//                field.getFieldInstance(vm, parent)
                field.ref
            }

            is InstanceDetails.DartList -> {
                val indexPath = path.pathToProperty.last() as PathToProperty.ListIndex
                coreEval.safeEval(mainIsolateId, "parent[${indexPath.index}]", mapOf("parent" to parent.instanceRefId))
            }

            is InstanceDetails.DartString -> {
                null
            }
        }

        if (currentRef == null) {
            throw RuntimeException("无法获取实例")
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
        val coreEval = EvalOnDartLibrary("dart:core", vm)
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

                val classRef = instance.getClassRef()
                val libraryUri = classRef.getLibrary()?.getUri() ?: "dart:core"
                val evalForInstance = EvalOnDartLibrary(libraryUri, vm)
                val allFields = mutableListOf<ObjectField>()
                var currentClass = coreEval.getClassObject(isolateId, classRef.getId()!!)
                val evalCache = mutableMapOf<String, EvalOnDartLibrary>()
                evalCache[libraryUri] = evalForInstance
                while (currentClass != null) {
                    currentClass.getFields().forEach { fieldRef: FieldRef ->
                        try {

                            val classRef: ObjRef = fieldRef.getOwner()

                            val owner: ClassObj? = vm.getObjectWithClassObj(isolateId, classRef.getId()!!)

                            val ownerUri: String = fieldRef.getLocation()!!.getScript().getUri()!!
                            val ownerName: String = (owner?.getMixin()?.getName() ?: owner?.getName()) ?: return@forEach

                            val ownerPackageName: String? = tryParsePackageName(ownerUri)
                            val isolate: Isolate = vm.getIsolateByIdPub(vm.getMainIsolates()!!.getId()!!)!!
                            val appName: String? = tryParsePackageName(isolate.getRootLib()!!.getUri()!!)

                            allFields.add(
                                ObjectField(
                                    name = fieldRef.getName(),
                                    isFinal = fieldRef.isFinal(),
                                    ownerName = ownerName,
                                    eval = EvalOnDartLibrary(
                                        ownerUri,
                                        vm
                                    ),
                                    ref = fieldRef.getDeclaredType(),
                                    ownerUri = ownerUri,
                                    isDefinedByDependency = ownerPackageName != appName,
                                    isStatic = fieldRef.isStatic()
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

private fun tryParsePackageName(uri: String): String? {
    return Regex("package:(.+?)/").find(uri)?.groupValues?.get(1)
}