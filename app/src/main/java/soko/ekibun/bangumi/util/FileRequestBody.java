package soko.ekibun.bangumi.util;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * 扩展OkHttp的请求体，实现上传时的进度提示
 *
 * @param <T>
 */
public final class FileRequestBody<T> extends RequestBody {
    /**
     * 实际请求体
     */
    private RequestBody requestBody;
    /**
     * 上传回调接口
     */
    private RetrofitCallback<T> callback;
    /**
     * 包装完成的BufferedSink
     */
    private BufferedSink bufferedSink;

    public FileRequestBody(RequestBody requestBody, RetrofitCallback<T> callback) {
        super();
        this.requestBody = requestBody;
        this.callback = callback;
    }

    @Override
    public long contentLength() throws IOException {
        return requestBody.contentLength();
    }

    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }

    @Override
    public void writeTo(@NotNull BufferedSink sink) throws IOException {
        if (bufferedSink == null) {
//包装
            bufferedSink = Okio.buffer(sink(sink));
        }

//写入
        requestBody.writeTo(bufferedSink);
//必须调用flush，否则最后一部分数据可能不会被写入
        bufferedSink.flush();
    }

    /**
     * 写入，回调进度接口
     *
     * @param sink Sink
     * @return Sink
     */
    private Sink sink(Sink sink) {
        return new ForwardingSink(sink) {
            //当前写入字节数
            long bytesWritten = 0L;
            //总字节长度，避免多次调用contentLength()方法
            long contentLength = 0L;

            @Override
            public void write(@NotNull Buffer source, long byteCount) throws IOException {
                long size = byteCount;
                if (contentLength == 0) {
                    //获得contentLength的值，后续不再调用
                    contentLength = contentLength();
                }
                while (size > 0) {
                    long sizeToWrite = Math.min(40960L, size);
                    super.write(source, sizeToWrite);
                    //增加当前写入的字节数
                    bytesWritten += sizeToWrite;
                    //回调
                    callback.onLoading(contentLength, bytesWritten);
                    size -= sizeToWrite;
                }
            }
        };
    }
}