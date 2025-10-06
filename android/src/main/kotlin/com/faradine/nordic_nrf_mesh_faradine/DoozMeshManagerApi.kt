package com.faradine.nordic_nrf_mesh_faradine

import android.content.Context
import android.util.Log
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import no.nordicsemi.android.mesh.MeshManagerApi
import no.nordicsemi.android.mesh.MeshNetwork
import no.nordicsemi.android.mesh.transport.*
import no.nordicsemi.android.mesh.utils.OutputOOBAction
import java.util.*
import kotlin.collections.ArrayList


class DoozMeshManagerApi(context: Context, binaryMessenger: BinaryMessenger) : StreamHandler, MethodChannel.MethodCallHandler {
    private var mMeshManagerApi: MeshManagerApi = MeshManagerApi(context.applicationContext)
    private var eventSink: EventSink? = null
    private var doozMeshNetwork: DoozMeshNetwork? = null
    private val doozMeshManagerCallbacks: DoozMeshManagerCallbacks
    private val doozMeshProvisioningStatusCallbacks: DoozMeshProvisioningStatusCallbacks
    private var doozMeshStatusCallbacks: DoozMeshStatusCallbacks
    private val unProvisionedMeshNodes: ArrayList<DoozUnprovisionedMeshNode> = ArrayList()
    var currentProvisionedMeshNode: DoozProvisionedMeshNode? = null
    private val tag: String = DoozMeshManagerApi::class.java.simpleName

    init {
        EventChannel(binaryMessenger, "$namespace/mesh_manager_api/events").setStreamHandler(this)
        MethodChannel(binaryMessenger, "$namespace/mesh_manager_api/methods").setMethodCallHandler(this)

        doozMeshManagerCallbacks = DoozMeshManagerCallbacks(binaryMessenger, eventSink)
        doozMeshProvisioningStatusCallbacks = DoozMeshProvisioningStatusCallbacks(binaryMessenger, eventSink, unProvisionedMeshNodes, this)
        doozMeshStatusCallbacks = DoozMeshStatusCallbacks(eventSink)

        mMeshManagerApi.setMeshManagerCallbacks(doozMeshManagerCallbacks)
        mMeshManagerApi.setProvisioningStatusCallbacks(doozMeshProvisioningStatusCallbacks)
        mMeshManagerApi.setMeshStatusCallbacks(doozMeshStatusCallbacks)
    }

    private fun loadMeshNetwork() {
        mMeshManagerApi.loadMeshNetwork()
    }

    // calls handleNotifications in MeshManagerApi.java
    // Processes and reassembles bluetooth mesh notifications
    private fun handleNotifications(mtu: Int, pdu: ByteArray) {
        mMeshManagerApi.handleNotifications(mtu, pdu)
    }

    private fun handleWriteCallbacks(mtu: Int, pdu: ByteArray) {
        mMeshManagerApi.handleWriteCallbacks(mtu, pdu)
    }

    private fun importMeshNetworkJson(json: String) {
        mMeshManagerApi.importMeshNetworkJson(json)
        return
    }

    private fun isNetworkImportInProgress(): Boolean {
        return mMeshManagerApi.getIsNetworkImportInProgress()
    }

    private fun importMeshNetworkFromQr(uuid: String, netkeys: List<ByteArray>, appkeys: List<ByteArray>, unicastLow: Int, unicastHight: Int, groupLow: Int, groupHigh: Int, sceneLow: Int, sceneHigh: Int) {
        mMeshManagerApi.importMeshNetworkFromQr(uuid, netkeys, appkeys, unicastLow, unicastHight, groupLow, groupHigh, sceneLow, sceneHigh)
    }

    private fun exportMeshNetwork(): String? {
        return mMeshManagerApi.exportMeshNetwork()
    }

    private fun deleteMeshNetworkFromDb(meshNetworkId: String) {
        if (mMeshManagerApi.meshNetwork?.meshUUID == meshNetworkId) {
            val meshNetwork: MeshNetwork = doozMeshNetwork!!.meshNetwork
            mMeshManagerApi.deleteMeshNetworkFromDb(meshNetwork)
        }
    }

    override fun onListen(arguments: Any?, events: EventSink?) {
        Log.d(tag, "onListen $arguments $events")
        this.eventSink = events
        doozMeshManagerCallbacks.eventSink = eventSink
        doozMeshProvisioningStatusCallbacks.eventSink = eventSink
        doozMeshStatusCallbacks.eventSink = eventSink
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
        doozMeshManagerCallbacks.eventSink = null
        doozMeshProvisioningStatusCallbacks.eventSink = null
        doozMeshStatusCallbacks.eventSink = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "loadMeshNetwork" -> {
                loadMeshNetwork()
                result.success(null)
            }
            "importMeshNetworkJson" -> {
                importMeshNetworkJson(call.argument<String>("json")!!)
                result.success(null)
            }
            "isNetworkImportInProgress" -> {
                val networkImportInProgress = isNetworkImportInProgress()
                result.success(networkImportInProgress)
            }
            "importMeshNetworkFromQr" -> {
                val uuid = call.argument<String>("uuid")!!
                val netkeysList = call.argument<List<List<Int>>>("netkeys") ?: emptyList()

                val netkeys: List<ByteArray> = netkeysList.map { innerList ->
                    innerList.map { it.toByte() }.toByteArray()
                }

                // just print stuff
                // Log.d(tag, "Net key data:")
                // netkeys.forEach { byteArray ->
                //     Log.d(tag, byteArray.joinToString(", ") { it.toString() })
                // }

                val appkeysList = call.argument<List<List<Int>>>("appkeys") ?: emptyList()

                val appkeys: List<ByteArray> = appkeysList.map { innerList ->
                    innerList.map { it.toByte() }.toByteArray()
                }

                val unicastLow = call.argument<Int>("unicastLow")!!
                val unicastHigh = call.argument<Int>("unicastHigh")!!
                val groupLow = call.argument<Int>("groupLow")!!
                val groupHigh = call.argument<Int>("groupHigh")!!
                val sceneLow = call.argument<Int>("sceneLow")!!
                val sceneHigh = call.argument<Int>("sceneHigh")!!

//                val netkeys = List<ByteArray>[]
//                for (netkeyData in netkeysList) {
//                    val netkey = ByteArray(netkeyData.size)
//                    for (i in netkeyData.indices) {
//                        netkey[i] = netkeyData[i].toByte()
//                    }
//                    netkeys.add...
//                }



//                importMeshNetworkJson(call.argument<String>("json")!!)
                importMeshNetworkFromQr(uuid, netkeys, appkeys, unicastLow, unicastHigh, groupLow, groupHigh, sceneLow, sceneHigh)
                result.success(null)
            }
            "deleteMeshNetworkFromDb" -> {
                deleteMeshNetworkFromDb(call.argument<String>("id")!!)
                result.success(null)
            }
            "exportMeshNetwork" -> {
                val json = exportMeshNetwork()
                result.success(json)
            }
            "resetMeshNetwork" -> {
                mMeshManagerApi.resetMeshNetwork()
                result.success(null)
            }
            "identifyNode" -> {
                mMeshManagerApi.identifyNode(UUID.fromString(call.argument<String>("serviceUuid")!!))
                result.success(null)
            }
            "identifyNodeWithTimer" -> {
                mMeshManagerApi.identifyNode(UUID.fromString(call.argument<String>("serviceUuid")!!), call.argument<Int>("attentionTimer")!!)
                result.success(null)
            }
            "getSequenceNumberForAddress" -> {
                val address = call.argument<Int>("address")!!
                val pNode = mMeshManagerApi.meshNetwork!!.getNode(address)
                result.success(pNode.sequenceNumber)
            }
            "setSequenceNumberForAddress" -> {
                val address = call.argument<Int>("address")!!
                val sequenceNumber = call.argument<Int>("sequenceNumber")!!
                val currentMeshNetwork = mMeshManagerApi.meshNetwork!!
                val pNode: ProvisionedMeshNode = currentMeshNetwork.getNode(address)
                pNode.sequenceNumber = sequenceNumber
                result.success(null)
            }
            "sendConfigModelAppBind" -> {
                val nodeId = call.argument<Int>("nodeId")!!
                val elementId = call.argument<Int>("elementId")!!
                val modelId = call.argument<Int>("modelId")!!
                val appKeyIndex = call.argument<Int>("appKeyIndex")!!
                val configModelAppBind = ConfigModelAppBind(elementId, modelId, appKeyIndex)
                mMeshManagerApi.createMeshPdu(nodeId, configModelAppBind)
                result.success(null)
            }
            "sendGenericLevelSet" -> {
                val sequenceNumber = getSequenceNumber(mMeshManagerApi.meshNetwork)
                val address = call.argument<Int>("address")!!
                val level = call.argument<Int>("level")!!
                val keyIndex = call.argument<Int>("keyIndex")!!
                val transitionStep = call.argument<Int>("transitionStep")
                val transitionResolution = call.argument<Int>("transitionResolution")
                val delay = call.argument<Int>("delay")
                val meshMessage: MeshMessage = GenericLevelSet(
                        mMeshManagerApi.meshNetwork!!.getAppKey(keyIndex),
                        transitionStep,
                        transitionResolution,
                        delay,
                        level,
                        sequenceNumber
                )
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null)
            }

            // There are get functions for some of these
            "sendGenericLevelGet" -> {
                val address = call.argument<Int>("address")!!
                val keyIndex = call.argument<Int>("keyIndex")!!
                val meshMessage: MeshMessage = GenericLevelGet(
                        mMeshManagerApi.meshNetwork!!.getAppKey(keyIndex)
                )
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null)
            } 
            "sendGenericOnOffSet" -> {
                val address = call.argument<Int>("address")!!
                val value = call.argument<Boolean>("value")!!
                val keyIndex = call.argument<Int>("keyIndex")!!
                val sequenceNumber = call.argument<Int>("sequenceNumber")!!
                val transitionStep = call.argument<Int>("transitionStep")
                val transitionResolution = call.argument<Int>("transitionResolution")
                val delay = call.argument<Int>("delay")
                val meshMessage: MeshMessage = GenericOnOffSet(
                        mMeshManagerApi.meshNetwork!!.getAppKey(keyIndex),
                        value,
                        sequenceNumber,
                        transitionStep,
                        transitionResolution,
                        delay
                )
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null) // the success value shows up in mesh_manager_api.dart sendGenericOnOffSet()
                // Any changes here have to re-run the whole app to take effect (No hot restart or reload)
            }


            "sendVendorModelMessage" -> {
                val address = call.argument<Int>("address")!!
                val modelId = call.argument<Int>("modelId")!!
                val keyIndex = call.argument<Int>("keyIndex")!!
                val companyIdentifier = call.argument<Int>("companyIdentifier")!!
                val opCode = call.argument<Int>("opCode")!!
                // Retrieve parameters as a List<Int>
                val parametersList = call.argument<List<Int>>("parameters") ?: emptyList()
                
                // Convert List<Int> to ByteArray
                val parameters = ByteArray(parametersList.size)
                for (i in parametersList.indices) {
                    parameters[i] = parametersList[i].toByte()
                }
            
                val meshMessage: MeshMessage = VendorModelMessageUnacked(
                        mMeshManagerApi.meshNetwork!!.getAppKey(keyIndex),
                        modelId,
                        companyIdentifier,
                        opCode,
                        parameters
                        
                )
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(1) // the success value shows up in mesh_manager_api.dart
            }

            "sendGenericLocationGlobalGet" -> {
                val address = call.argument<Int>("address")!!
                val keyIndex = call.argument<Int>("keyIndex")!!
                val meshMessage: MeshMessage = GenericLocationGlobalGet(
                        mMeshManagerApi.meshNetwork!!.getAppKey(keyIndex),                        
                )
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(1) // the success value shows up in mesh_manager_api.dart
            }

            "getSNBeacon" -> {
                val address = call.argument<Int>("address")!!
                val meshMessage: MeshMessage = ConfigBeaconGet()
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null)
            }
            "setSNBeacon" -> {
                val address = call.argument<Int>("address")!!
                val enable = call.argument<Boolean>("enable")!!
                val meshMessage: MeshMessage = ConfigBeaconSet(enable)
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null)
            }
            "setNetworkTransmitSettings" -> {
                val address = call.argument<Int>("address")!!
                val transmitCount = call.argument<Int>("transmitCount")!!
                val transmitIntervalSteps = call.argument<Int>("transmitIntervalSteps")!!
                val meshMessage: MeshMessage = ConfigNetworkTransmitSet(
                    transmitCount,
                    transmitIntervalSteps
                )
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null)
            }
            "getNetworkTransmitSettings" -> {
                val address = call.argument<Int>("address")!!
                val meshMessage: MeshMessage = ConfigNetworkTransmitGet()
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null)
            }
            "getDefaultTtl" -> {
                val address = call.argument<Int>("address")!!
                val meshMessage: MeshMessage = ConfigDefaultTtlGet()
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null)
            }
            "setDefaultTtl" -> {
                val address = call.argument<Int>("address")!!
                val ttl = call.argument<Int>("ttl")!!
                val meshMessage: MeshMessage = ConfigDefaultTtlSet(ttl)
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null)
            }
            "keyRefreshPhaseGet" -> {
                val address = call.argument<Int>("address")!!
                val netKeyIndex = call.argument<Int>("netKeyIndex")!!
                val currentMeshNetwork = mMeshManagerApi.meshNetwork!!
                val meshMessage: MeshMessage = ConfigKeyRefreshPhaseGet(currentMeshNetwork.netKeys[netKeyIndex])
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null)
            }
            "keyRefreshPhaseSet" -> {
                val address = call.argument<Int>("address")!!
                val netKeyIndex = call.argument<Int>("netKeyIndex")!!
                val transition = call.argument<Int>("transition")!!
                val currentMeshNetwork = mMeshManagerApi.meshNetwork!!
                val meshMessage: MeshMessage = ConfigKeyRefreshPhaseSet(currentMeshNetwork.netKeys[netKeyIndex], transition)
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null)
            }
            "sendConfigModelSubscriptionAdd" -> {
                val elementAddress = call.argument<Int>("elementAddress")!!
                val subscriptionAddress = call.argument<Int>("subscriptionAddress")!!
                val modelIdentifier = call.argument<Int>("modelIdentifier")!!
                val currentMeshNetwork = mMeshManagerApi.meshNetwork!!
                val pNode: ProvisionedMeshNode = currentMeshNetwork.getNode(elementAddress)
                val meshMessage = ConfigModelSubscriptionAdd(elementAddress, subscriptionAddress, modelIdentifier)
                mMeshManagerApi.createMeshPdu(pNode.unicastAddress, meshMessage)
                result.success(null)
            }
            "sendConfigModelSubscriptionDelete" -> {
                val elementAddress = call.argument<Int>("elementAddress")!!
                val subscriptionAddress = call.argument<Int>("subscriptionAddress")!!
                val modelIdentifier = call.argument<Int>("modelIdentifier")!!
                val currentMeshNetwork = mMeshManagerApi.meshNetwork!!
                val pNode: ProvisionedMeshNode = currentMeshNetwork.getNode(elementAddress)
                val meshMessage = ConfigModelSubscriptionDelete(elementAddress, subscriptionAddress, modelIdentifier)
                mMeshManagerApi.createMeshPdu(pNode.unicastAddress, meshMessage)
                result.success(null)
            }
            "sendConfigModelSubscriptionDeleteAll" -> {
                val elementAddress = call.argument<Int>("elementAddress")!!
                val modelIdentifier = call.argument<Int>("modelIdentifier")!!
                val meshMessage = ConfigModelSubscriptionDeleteAll(elementAddress, modelIdentifier)
                mMeshManagerApi.createMeshPdu(elementAddress, meshMessage)
                result.success(null)
            }
            "sendConfigModelPublicationSet" -> {
                val elementAddress = call.argument<Int>("elementAddress")!!
                val publishAddress = call.argument<Int>("publishAddress")!!
                val appKeyIndex = call.argument<Int>("appKeyIndex")!!
                val credentialFlag = call.argument<Boolean>("credentialFlag")!!
                val publishTtl = call.argument<Int>("publishTtl")!!
                val publicationSteps = call.argument<Int>("publicationSteps")!!
                val publicationResolution = call.argument<Int>("publicationResolution")!!
                val retransmitCount = call.argument<Int>("retransmitCount")!!
                val retransmitIntervalSteps = call.argument<Int>("retransmitIntervalSteps")!!
                val modelIdentifier = call.argument<Int>("modelIdentifier")!!
                val currentMeshNetwork = mMeshManagerApi.meshNetwork!!
                val pNode: ProvisionedMeshNode = currentMeshNetwork.getNode(elementAddress)
                val meshMessage = ConfigModelPublicationSet(
                        elementAddress,
                        publishAddress,
                        appKeyIndex,
                        credentialFlag,
                        publishTtl,
                        publicationSteps,
                        publicationResolution,
                        retransmitCount,
                        retransmitIntervalSteps,
                        modelIdentifier
                )
                mMeshManagerApi.createMeshPdu(pNode.unicastAddress, meshMessage)
                result.success(null)
            }
            "doozScenarioSet" -> {
                val scenarioId = call.argument<Int>("scenarioId")!!
                val command = call.argument<Int>("command")!!
                val io = call.argument<Int>("io")!!
                val isActive = call.argument<Boolean>("isActive")!!
                val unused = call.argument<Int>("unused")!!
                val value = call.argument<Int>("value")!!
                val transition = call.argument<Int>("transition")!!
                val startAt = call.argument<Int>("startAt")!!
                val duration = call.argument<Int>("duration")!!
                val daysInWeek = call.argument<Int>("daysInWeek")!!
                val correlation = call.argument<Int>("correlation")!!
                val extra = call.argument<Int?>("extra")
                val address = call.argument<Int>("address")!!
                val keyIndex = call.argument<Int>("keyIndex")!!
                val sequenceNumber = getSequenceNumber(mMeshManagerApi.meshNetwork)
                val meshMessage = DoozScenarioSet(
                    mMeshManagerApi.meshNetwork!!.getAppKey(keyIndex),
                    scenarioId,
                    command,
                    io,
                    isActive,
                    unused,
                    value,
                    transition,
                    startAt,
                    duration,
                    daysInWeek,
                    correlation,
                    extra,
                    sequenceNumber,
                )
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null)
            }
            "doozScenarioEpochSet" -> {
                val packed = call.argument<Int>("packed")!!
                val epoch = call.argument<Int>("epoch")!!
                val correlation = call.argument<Int>("correlation")!!
                val extra = call.argument<Int?>("extra")
                val address = call.argument<Int>("address")!!
                val keyIndex = call.argument<Int>("keyIndex")!!
                val sequenceNumber = getSequenceNumber(mMeshManagerApi.meshNetwork)
                val meshMessage = DoozEpochSet(
                    mMeshManagerApi.meshNetwork!!.getAppKey(keyIndex),
                    packed,
                    epoch,
                    correlation,
                    extra,
                    sequenceNumber,
                )
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null)
            }
            "getPublicationSettings" -> {
                val elementAddress = call.argument<Int>("elementAddress")!!
                val modelIdentifier = call.argument<Int>("modelIdentifier")!!
                val currentMeshNetwork = mMeshManagerApi.meshNetwork!!
                val pNode: ProvisionedMeshNode = currentMeshNetwork.getNode(elementAddress)
                val meshMessage = ConfigModelPublicationGet(
                        elementAddress,
                        modelIdentifier
                )
                mMeshManagerApi.createMeshPdu(pNode.unicastAddress, meshMessage)
                result.success(null)
            }
            "sendV2MagicLevel" -> {
                val io = call.argument<Int>("io")!!
                val index = call.argument<Int>("index")!!
                val value = call.argument<Int>("value")!!
                val correlation = call.argument<Int>("correlation")!!
                val address = call.argument<Int>("address")!!
                val keyIndex = call.argument<Int>("keyIndex")!!
                val sequenceNumber = getSequenceNumber(mMeshManagerApi.meshNetwork)
                val meshMessage = MagicLevelSet(
                        mMeshManagerApi.meshNetwork!!.getAppKey(keyIndex),
                        io, index, value, correlation, sequenceNumber
                )
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null)
            }
            "getV2MagicLevel" -> {
                val io = call.argument<Int>("io")!!
                val index = call.argument<Int>("index")!!
                val correlation = call.argument<Int>("correlation")!!
                val address = call.argument<Int>("address")!!
                val keyIndex = call.argument<Int>("keyIndex")!!
                val sequenceNumber = getSequenceNumber(mMeshManagerApi.meshNetwork)
                val meshMessage = MagicLevelGet(
                        mMeshManagerApi.meshNetwork!!.getAppKey(keyIndex),
                        io, index, correlation, sequenceNumber
                )
                mMeshManagerApi.createMeshPdu(address, meshMessage)
                result.success(null)
            }

            // Lightness commands exist
            // Lots of command files here Android-nRF-Mesh-Library\mesh\src\main\java\no\nordicsemi\android\mesh\transport
            "sendLightLightness" -> {
                val sequenceNumber = call.argument<Int>("sequenceNumber")!!
                val address = call.argument<Int>("address")!!
                val keyIndex = call.argument<Int>("keyIndex")!!
                val lightness = call.argument<Int>("lightness")!!
                val lightnessSet = LightLightnessSet(
                        mMeshManagerApi.meshNetwork!!.getAppKey(keyIndex),
                        lightness,
                        sequenceNumber)
                mMeshManagerApi.createMeshPdu(address, lightnessSet)
                result.success(null)
            }
            "sendLightCtl" -> {
                val sequenceNumber = call.argument<Int>("sequenceNumber")!!
                val address = call.argument<Int>("address")!!
                val keyIndex = call.argument<Int>("keyIndex")!!
                val lightness = call.argument<Int>("lightness")!!
                val temperature = call.argument<Int>("temperature")!!
                val lightDeltaUV = call.argument<Int>("lightDeltaUV")!!
                val lightCtlSet = LightCtlSet(
                        mMeshManagerApi.meshNetwork!!.getAppKey(keyIndex),
                        lightness,
                        temperature,
                        lightDeltaUV,
                        sequenceNumber)
                mMeshManagerApi.createMeshPdu(address, lightCtlSet)
                result.success(null)
            }
            "sendLightHsl" -> {
                val sequenceNumber = call.argument<Int>("sequenceNumber")!!
                val address = call.argument<Int>("address")!!
                val keyIndex = call.argument<Int>("keyIndex")!!
                val lightness = call.argument<Int>("lightness")!!
                val hue = call.argument<Int>("hue")!!
                val saturation = call.argument<Int>("saturation")!!
                val lightHslSet = LightHslSet(mMeshManagerApi.meshNetwork!!.getAppKey(keyIndex),
                        lightness, hue, saturation, sequenceNumber)
                mMeshManagerApi.createMeshPdu(address, lightHslSet)
                result.success(null)
            }
            "getDeviceUuid" -> {
                val serviceData = call.argument<ByteArray>("serviceData")!!
                result.success(mMeshManagerApi.getDeviceUuid(serviceData).toString())
            }

            // Calls private fun handleNotifications
            "handleNotifications" -> {
                val pdu = call.argument<ByteArray>("pdu")!!
                handleNotifications(call.argument<Int>("mtu")!!, pdu)
                result.success(null)
            }
            "handleWriteCallbacks" -> {
                val pdu = call.argument<ArrayList<Int>>("pdu")!!
                handleWriteCallbacks(call.argument<Int>("mtu")!!, arrayListToByteArray(pdu))
                result.success(null)
            }
            "cleanProvisioningData" -> {
                unProvisionedMeshNodes.clear()
                currentProvisionedMeshNode = null
                result.success(null)
            }
            "provisioning" -> {
                val uuid = UUID.fromString(call.argument("uuid")!!)
//                val deviceId = call.argument("deviceId")!!
                val unProvisionedMeshNode = unProvisionedMeshNodes.firstOrNull { it.meshNode.deviceUuid == uuid }
                if (unProvisionedMeshNode == null) {
                    result.error("NOT_FOUND", "MeshNode with uuid $uuid doesn't exist", null)
                    return
                }
                mMeshManagerApi.startProvisioning(unProvisionedMeshNode.meshNode)
                result.success(null)
            }
            "provisioningWithOutputOOBNumeric" -> {
                val uuid = UUID.fromString(call.argument("uuid")!!)
                val unProvisionedMeshNode = unProvisionedMeshNodes.firstOrNull { it.meshNode.deviceUuid == uuid }
                if (unProvisionedMeshNode == null) {
                    result.error("NOT_FOUND", "MeshNode with uuid $uuid doesn't exist", null)
                    return
                }
                // OOB provisioning action constants from Android-nRF-Mesh-Library\mesh\src\main\java\no\nordicsemi\android\mesh\utils\OutputOOBAction.java
                mMeshManagerApi.startProvisioningWithOutputOOB(unProvisionedMeshNode.meshNode, OutputOOBAction.OUTPUT_NUMERIC)
                result.success(null)
            }
            "setProvisioningAuthentication"-> {
                val authentication = call.argument<String>("authentication")!!

                mMeshManagerApi.setProvisioningAuthentication(authentication)
                result.success(null)
            }
            "cachedProvisionedMeshNodeUuid" -> {
                if (null == currentProvisionedMeshNode) {
                    result.success(null)
                } else {
                    val provisionedMeshNode = currentProvisionedMeshNode!!
                    result.success(provisionedMeshNode.meshNode.getUuid())
                }
            }
            "deprovision" -> {
                try {
                    val unicastAddress = call.argument<Int>("unicastAddress")!!
                    val currentMeshNetwork = mMeshManagerApi.meshNetwork!!
                    val pNode: ProvisionedMeshNode = currentMeshNetwork.getNode(unicastAddress)
                    if (pNode == null) {
                        result.error("NOT_FOUND", "MeshNode with unicastAddress $unicastAddress doesn't exist", null)
                    } else {
                        Log.d(tag, "should unprovision the nodeId : " + unicastAddress)
                        val configNodeReset = ConfigNodeReset()
                        mMeshManagerApi.createMeshPdu(unicastAddress, configNodeReset)
                    }
                } catch (ex: Exception) {
                    Log.e(tag, ex.message.toString())
                    result.success(false)
                }
                result.success(true)
            }
            "meshNodeReset" -> {
                val unicastAddress = call.argument<Int>("unicastAddress")!!
                val currentMeshNetwork = mMeshManagerApi.meshNetwork!!
                val pNode: ProvisionedMeshNode = currentMeshNetwork.getNode(unicastAddress)
                mMeshManagerApi.onMeshNodeReset(pNode);
                result.success(true)
            }
            "sendConfigCompositionDataGet" -> {
                mMeshManagerApi.createMeshPdu(call.argument("dest")!!, ConfigCompositionDataGet())
                result.success(null)
            }
            "sendConfigAppKeyAdd" -> {
                val currentMeshNetwork = mMeshManagerApi.meshNetwork!!
                val configAppKeyAdd = ConfigAppKeyAdd(currentMeshNetwork.netKeys[0], currentMeshNetwork.appKeys[0])
                mMeshManagerApi.createMeshPdu(call.argument("dest")!!, configAppKeyAdd)
                result.success(null)
            }
            "setMtuSize" -> {
                doozMeshManagerCallbacks.mtuSize = call.argument<Int>("mtuSize")!!
                result.success(null)
            }
            "nodeIdentityMatches" -> {
                val serviceData = call.argument<ByteArray>("serviceData")!!
                val currentMeshNetwork = mMeshManagerApi.meshNetwork!!
                if(mMeshManagerApi.isAdvertisedWithNodeIdentity(serviceData)){
                    currentMeshNetwork.nodes.forEach { node ->
                        if (mMeshManagerApi.nodeIdentityMatches(node, serviceData)) {
                            result.success(true)
                        }
                    }
                }
                result.success(false)
            }
            "networkIdMatches" -> {
                val serviceData = call.argument<ByteArray>("serviceData")!!
                val currentMeshNetwork = mMeshManagerApi.meshNetwork!!
                val networkKeys = currentMeshNetwork.getNetKeys()!!
                val networkId = mMeshManagerApi.generateNetworkId(networkKeys.get(0).getKey())
                var matches = mMeshManagerApi.networkIdMatches(networkId, serviceData)
                result.success(matches)
            }
            "isAdvertisingWithNetworkIdentity" -> {
                val serviceData = call.argument<ByteArray>("serviceData")!!
                try {
                    result.success(mMeshManagerApi.isAdvertisingWithNetworkIdentity(serviceData))
                } catch (e: Exception) {
                    result.error("101", e.message, "an error occured while checking service data")
                }
            }
            "isAdvertisedWithNodeIdentity" -> {
                val serviceData = call.argument<ByteArray>("serviceData")!!
                try {
                    result.success(mMeshManagerApi.isAdvertisedWithNodeIdentity(serviceData))
                } catch (e: Exception) {
                    result.error("102", e.message, "an error occured while checking service data")
                }
            }

            // Custom stuff
            "customTest" -> {
                result.success(1)
            }

            // Final catch all
            else -> {
                result.notImplemented()
            }
        }
    }
}

