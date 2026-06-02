package com.dogear.reader.format.api.content

/**
 * Typed open failures so the reader can show a clear, specific message instead of a generic
 * error (Hardening research: distinguish DRM / password / corrupt / missing). Handlers throw
 * these from [com.dogear.reader.format.api.BookFormatHandler.openContent]; the reader classifies
 * them. We never attempt to bypass DRM.
 */
class DrmProtectedException(
    message: String = "This book is DRM-protected and can't be opened here.",
) : Exception(message)

class PasswordRequiredException(
    message: String = "This file is password-protected.",
) : Exception(message)

class CorruptBookException(
    message: String = "This file appears to be corrupt or unreadable.",
) : Exception(message)
