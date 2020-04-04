module jpms.k8s.app {
    // jdbc utility
    requires commons.dbutils;
    // jdbc driver
    requires org.mariadb.jdbc;
    // logging
    requires java.logging;
    // SQL
    requires java.sql;
}