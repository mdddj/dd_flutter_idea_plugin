package shop.itbug.flutterx.api.vm

import vm.VmService

interface DartVmApp {
    val appId: String
    val vmUrl: String
    val deviceId: String
    val mode: String
    val vmService: VmService
}
