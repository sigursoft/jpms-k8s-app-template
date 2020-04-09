module jpms.k8s.app {
    // logging
    requires java.logging;
    // http server
    requires jdk.httpserver;
    // http client
    requires java.net.http;
    // for https
    requires jdk.crypto.ec;
    // safe money utils
    requires org.joda.money;
}