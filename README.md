# android-sockets-icmp-ping-test
This is a sample application that sends ICMP packets using sockets and expects a response from the remote server (ping)

It demonstrates difference in behavior of `Os.sendto` on different APIs (22 and higher)

It works on the Android API higher than 22 and throws an exception when launched on API 22 (Lollipop)
