package net.mclorehub.importer;

import java.io.IOException;

public final class ImportException extends IOException {
    public ImportException(String message) { super(message); }
    public ImportException(String message, Throwable cause) { super(message, cause); }
}
