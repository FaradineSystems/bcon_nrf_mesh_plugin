package no.nordicsemi.android.mesh.transport;

import no.nordicsemi.android.mesh.logger.MeshLogger;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import no.nordicsemi.android.mesh.ApplicationKey;
import no.nordicsemi.android.mesh.InternalTransportCallbacks;
import no.nordicsemi.android.mesh.MeshStatusCallbacks;

class VendorModelMessageUnackedState extends ApplicationMessageState {

    private static final String TAG = VendorModelMessageUnackedState.class.getSimpleName();

    /**
     * Constructs {@link VendorModelMessageAckedState}
     *
     * @param src                       Source address
     * @param dst                       Destination address to which the message must be sent to
     * @param vendorModelMessageUnacked Wrapper class {@link VendorModelMessageStatus} containing the
     *                                  opcode and parameters for {@link VendorModelMessageStatus} message
     * @param handlerCallbacks          {@link InternalMeshMsgHandlerCallbacks} for internal callbacks
     * @param transportCallbacks        {@link InternalTransportCallbacks} callbacks
     * @param statusCallbacks           {@link MeshStatusCallbacks} callbacks
     * @throws IllegalArgumentException exception for invalid arguments
     */
    VendorModelMessageUnackedState(final int src,
                                   final int dst,
                                   @NonNull final VendorModelMessageUnacked vendorModelMessageUnacked,
                                   @NonNull final MeshTransport meshTransport,
                                   @NonNull final InternalMeshMsgHandlerCallbacks handlerCallbacks,
                                   @NonNull final InternalTransportCallbacks transportCallbacks,
                                   @NonNull  final MeshStatusCallbacks statusCallbacks) throws IllegalArgumentException {
        this(src, dst, null, vendorModelMessageUnacked, meshTransport, handlerCallbacks, transportCallbacks, statusCallbacks);
    }

    /**
     * Constructs {@link VendorModelMessageAckedState}
     *
     * @param src                       Source address
     * @param dst                       Destination address to which the message must be sent to
     * @param label                     Label UUID of destination address
     * @param vendorModelMessageUnacked Wrapper class {@link VendorModelMessageStatus} containing the
     *                                  opcode and parameters for {@link VendorModelMessageStatus} message
     * @param handlerCallbacks          {@link InternalMeshMsgHandlerCallbacks} for internal callbacks
     * @param transportCallbacks        {@link InternalTransportCallbacks} callbacks
     * @param statusCallbacks           {@link MeshStatusCallbacks} callbacks
     * @throws IllegalArgumentException exception for invalid arguments
     */
    VendorModelMessageUnackedState(final int src,
                                   final int dst,
                                   @Nullable UUID label,
                                   @NonNull final VendorModelMessageUnacked vendorModelMessageUnacked,
                                   @NonNull final MeshTransport meshTransport,
                                   @NonNull final InternalMeshMsgHandlerCallbacks handlerCallbacks,
                                   @NonNull final InternalTransportCallbacks transportCallbacks,
                                   @NonNull  final MeshStatusCallbacks statusCallbacks) throws IllegalArgumentException {
        super(src, dst, vendorModelMessageUnacked, meshTransport, handlerCallbacks, transportCallbacks, statusCallbacks);
    }

    @Override
    public MeshMessageState.MessageState getState() {
        return MessageState.VENDOR_MODEL_UNACKNOWLEDGED_STATE;
    }

    @Override
    protected final void createAccessMessage() {
        final VendorModelMessageUnacked message = (VendorModelMessageUnacked) mMeshMessage;
        final ApplicationKey key = message.getAppKey();
        final int akf = message.getAkf();
        final int aid = message.getAid();
        final int aszmic = message.getAszmic();
        final int opCode = message.getOpCode();
        final byte[] parameters = message.getParameters();
        final int companyIdentifier = message.getCompanyIdentifier();
        this.message = mMeshTransport.createVendorMeshMessage(companyIdentifier, mSrc, mDst, mLabel, message.messageTtl,
                key, akf, aid, aszmic, opCode, parameters);
        message.setMessage(this.message);
    }

    @Override
    public void executeSend() {
        MeshLogger.verbose(TAG, "Sending unacknowledged vendor model message");
        super.executeSend();
    }
}
