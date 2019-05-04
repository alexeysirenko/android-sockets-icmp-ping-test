package com.example.socket_test

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.net.Inet6Address
import java.net.InetAddress

class MainActivity : AppCompatActivity() {

    val tag = this.javaClass.canonicalName

    private val timeoutMs = 5000
    private val delayMs = 500L

    private val ECHO_PORT =  80
    private val POLLIN = (if (OsConstants.POLLIN == 0) 1 else OsConstants.POLLIN).toShort()
    private val MSG_DONTWAIT = 0x40


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (android.os.Build.VERSION.SDK_INT > 9) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }

        startPingButton.setOnClickListener {
            val host = addressText.text.toString()
            ping(host)
        }
    }

    fun ping(host: String): Unit {
        val inetAddress: InetAddress = InetAddress.getByName(host)
        if (inetAddress is Inet6Address) throw Exception("IPv6 implementation omitted for simplicity")
        val proto = OsConstants.IPPROTO_ICMP
        val inet = OsConstants.AF_INET
        val type = PacketBuilder.TYPE_ICMP_V4
        val socketFileDescriptor = Os.socket(inet, OsConstants.SOCK_DGRAM, proto)
        if (!socketFileDescriptor.valid()) throw Exception("Socket descriptor is invalid")
        try {
            val structPollfd = StructPollfd()
            structPollfd.fd = socketFileDescriptor
            structPollfd.events = POLLIN
            val structPollfds = arrayOf(structPollfd)
            var sequenceNumber: Short = 0
            for (i in 0..2) {
                sequenceNumber++
                val echoPacketBuilder =
                    PacketBuilder(type, "foobarbazquok".toByteArray())
                        .withSequenceNumber(sequenceNumber)
                val buffer = echoPacketBuilder.build()

                try {
                    /**
                     * This is the command that throws an exception
                     */
                    val bytesSent = Os.sendto(socketFileDescriptor, buffer,0, buffer.size, 0, inetAddress, ECHO_PORT)
                    val start = System.currentTimeMillis() // TODO: before or after `sendTo`?
                    if (bytesSent >= 0) {
                        val bytesReceived = Os.poll(structPollfds, timeoutMs)
                        val time = System.currentTimeMillis() - start
                        if (bytesReceived >= 0) {
                            if (structPollfd.revents == POLLIN) {
                                structPollfd.revents = 0
                                val messageBytesReceived =
                                    Os.recvfrom(socketFileDescriptor, buffer, 0, buffer.size, MSG_DONTWAIT, null)
                                if (messageBytesReceived < 0) {
                                    append("recvfrom() return failure: $messageBytesReceived")
                                }
                                append("$messageBytesReceived bytes icmp_seq=$sequenceNumber time=$time\n")
                            } else {
                                append("icmp_seq=$sequenceNumber timed out")
                            }
                        } else {
                            append("poll() failed")
                        }
                    } else {
                        append("sendto() failed")
                    }
                    Thread.sleep(delayMs)
                } catch (e: Exception) {
                    append("Error: " + e.message.orEmpty())
                    Log.e(tag, "error", e)
                }
            }
        }  finally {
            Os.close(socketFileDescriptor)
        }
    }

    private fun append(line: String) {
        consoleTextView.append(line + "\n")
    }

}
