package net.hostsharing.hsadminng.context;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class HttpServletRequestBodyCache extends ServletInputStream {

    private InputStream inputStream;

    public HttpServletRequestBodyCache(byte[] body) {
        this.inputStream = new ByteArrayInputStream(body);
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public boolean isFinished() {
        try {
            return available() == 0;
        } catch (IOException e) {
            return true;
        }
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(final ReadListener listener) {
        throw new RuntimeException("Not implemented");
    }
}
