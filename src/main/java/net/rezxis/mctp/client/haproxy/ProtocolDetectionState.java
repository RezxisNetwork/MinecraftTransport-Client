package net.rezxis.mctp.client.haproxy;

public enum ProtocolDetectionState {
    /**
     * Need more data to detect the protocol.
     */
    NEEDS_MORE_DATA,

    /**
     * The data was invalid.
     */
    INVALID,

    /**
     * Protocol was detected,
     */
    DETECTED
}