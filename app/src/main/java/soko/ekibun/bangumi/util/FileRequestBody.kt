package soko.ekibun.bangumi.util

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.*
import java.io.IOException

/**
 * 扩展OkHttp的请求体，实现上传时的进度提示
 */
class FileRequestBody(
    private val requestBody: RequestBody, private val callback: (total: Long, progress: Long) -> Unit
) : RequestBody() {

    /**
     * 包装完成的BufferedSink
     */
    private var bufferedSink: BufferedSink? = null

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return requestBody.contentLength()
    }

    override fun contentType(): MediaType? {
        return requestBody.contentType()
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        bufferedSink = bufferedSink ?: sink(sink).buffer() //包装
        requestBody.writeTo(bufferedSink!!) //写入
        bufferedSink!!.flush() //必须调用flush，否则最后一部分数据可能不会被写入
    }

    /**
     * 写入，回调进度接口
     *
     * @param sink Sink
     * @return Sink
     */
    private fun sink(sink: Sink): Sink {
        return object : ForwardingSink(sink) {
            //当前写入字节数
            var bytesWritten = 0L

            //总字节长度，避免多次调用contentLength()方法
            var contentLength = 0L

            @Throws(IOException::class)
            override fun write(source: Buffer, byteCount: Long) {
                var size = byteCount
                if (contentLength == 0L) {
                    //获得contentLength的值，后续不再调用
                    contentLength = contentLength()
                }
                while (size > 0) {
                    val sizeToWrite = Math.min(40960L, size)
                    super.write(source, sizeToWrite)
                    //增加当前写入的字节数
                    bytesWritten += sizeToWrite
                    //回调
                    callback(contentLength, bytesWritten)
                    size -= sizeToWrite
                }
            }
        }
    }

}