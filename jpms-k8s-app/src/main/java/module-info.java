module jpms.k8s.app {
    // logging
    requires java.logging;
    // http server
    requires jdk.httpserver;
    // http client
    requires java.net.http;
    // for https
    requires jdk.crypto.ec;
    // slf4j
    requires org.slf4j;
    requires org.slf4j.jul;
    requires com.fasterxml.jackson.core;
}