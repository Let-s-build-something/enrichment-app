package data.shared.crypto.verification

sealed interface SelfVerificationMethods {
        /**
         * We don't have enough information yet to calculated available methods (e.g. waiting for the first sync).
         */
        data class PreconditionsNotMet(val reasons: Set<Reason>) : SelfVerificationMethods {
            interface Reason {
                data object SyncNotRunning : Reason
                data object DeviceKeysNotFetchedYet : Reason
                data object CrossSigningKeysNotFetchedYet : Reason
            }
        }

        /**
         * Cross signing can be bootstrapped.
         * Bootstrapping can be done with [KeyService::bootstrapCrossSigning][net.folivo.trixnity.client.key.KeyServiceImpl.bootstrapCrossSigning].
         */
        data object NoCrossSigningEnabled : SelfVerificationMethods

        /**
         * No self verification needed.
         */
        data object AlreadyCrossSigned : SelfVerificationMethods

        /**
         * If empty: no other device & no key backup -> consider new bootstrapping of cross signing
         */
        data class CrossSigningEnabled(val methods: Set<SelfVerificationMethod>) :
            SelfVerificationMethods
    }