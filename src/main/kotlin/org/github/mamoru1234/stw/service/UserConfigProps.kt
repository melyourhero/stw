package org.github.mamoru1234.stw.service

/**
 * Path with cloud sources
 */
const val CLOUD_PATH = "cloud.path"

/**
 * User login to docker registry
 */
const val REGISTRY_USER = "registry.user"

/**
 * Password to docker registry
 */
const val REGISTRY_PASS = "registry.pass"

/**
 * Path to file with additional cloud env properties(optional)
 */
const val CLOUD_ENV_FILE_PATH = "cloud.env.file.path"

const val RIOT_PROXY_CONFIG_PATH = "riot.proxy.config.path"

/**
 * Local machine IP address (optional). Determined by request to google.
 */
const val MACHINE_IP = "machine.ip"

/**
 * Interval in millis for cloud container health check(optional). Default 5 sec.
 */
const val CLOUD_HEALTH_SLEEP = "cloud.health.sleep"

const val ATOM_IMAGE = "atom.image"
const val ATOM_DISABLE_ANALYTICS = "atom.disableAnalytics"

const val CLOUD_MQTT_URL = "csvAdapter.cloudUrl"
const val CLOUD_MAX_FILE_SIZE = "cloud.maxFileSize"

const val CSV_ADAPTER_IMAGE = "csv.adapter.image"
const val CSV_ADAPTER_DATA_PATH = "csv.adapter.data.path"

const val CASSANDRA_XMX = "cloud.cassandra.xmx"
const val CASSANDRA_XMS = "cloud.cassandra.xms"
