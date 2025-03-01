package data.shared.crypto

import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.keys.CrossSigningKeys
import net.folivo.trixnity.core.model.keys.Signed

typealias SignedCrossSigningKeys = Signed<CrossSigningKeys, UserId>