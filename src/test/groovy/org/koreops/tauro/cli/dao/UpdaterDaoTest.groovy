package org.koreops.tauro.cli.dao

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class UpdaterDaoTest extends Specification {

    @Shared
    def SSID_VALUE = "H4ckMe"
    @Shared
    def BSSID_VALUE = "66:66:66:66:66:66"
    @Shared
    def HOST = "dummy_host"
    @Shared
    def WPA_AUTH_TYPE = "WPA"
    @Shared
    def OPEN_AUTH_TYPE = "OPEN"
    @Shared
    def ENCRYPTION = "AES"
    @Shared
    def KEY = "123456"
    @Shared
    def PHONE = "123456"
    @Shared
    def ISP = "deutsche telekom"

    @Shared
    def WIFI_DATA = ["SSID": SSID_VALUE, "BSSID": BSSID_VALUE, "key": KEY, "Phone": PHONE]

    @Shared
    def STATION_EXISTS = true

    @Shared
    def STATION_DOES_NOT_EXIST = false

    @Shared
    def PROTECTED_NETWORK_WIFI_DATA = ["AuthType": WPA_AUTH_TYPE, "Encryption": ENCRYPTION] + WIFI_DATA

    @Shared
    def OPEN_NETWORK_WIFI_DATA = ["AuthType": OPEN_AUTH_TYPE] + WIFI_DATA


    def "test that the prepared statement responsible for finding a station is correctly assembled"() {
        given: "a record of a station in the db"
        def findStationResult = Mock(ResultSet) {
            next() >> STATION_EXISTS
        }
        def findStationPreparedStatement = Mock(PreparedStatement) {
            1 * executeQuery() >> findStationResult
        }
        def dbConnection = Mock(Connection) {
            prepareStatement(UpdaterDao.FIND_STATION_SQL_STATEMENT) >> findStationPreparedStatement
            prepareStatement(UpdaterDao.UPDATE_STATION_SQL_STATEMENT) >> Mock(PreparedStatement)
        }
        when: "an attempt to save a station to the db is made"
        UpdaterDao.saveStation(WIFI_DATA, HOST, dbConnection)
        then: "the prepared statement is correctly assembled with the rught arguments"
        1 * findStationPreparedStatement.setString(1, BSSID_VALUE)
        1 * findStationPreparedStatement.setString(2, SSID_VALUE)
        2 * findStationPreparedStatement.close() // this should be called only once
        1 * findStationResult.close()
    }

    @Unroll
    def "Test that an existing station is correctly updated for a #description"() {
        given:
        def findStationResult = Mock(ResultSet) {
            next() >> STATION_EXISTS
        }
        def findStationPreparedStatement = Mock(PreparedStatement) {
            executeQuery() >> findStationResult
        }
        def updateStationPreparedStatement = Mock(PreparedStatement)

        def dbConnection = Mock(Connection) {
            prepareStatement(UpdaterDao.FIND_STATION_SQL_STATEMENT) >> findStationPreparedStatement
            prepareStatement(UpdaterDao.UPDATE_STATION_SQL_STATEMENT) >> updateStationPreparedStatement
        }
        when:
        UpdaterDao.setIsp(ISP)
        UpdaterDao.saveStation(wifiData, HOST, dbConnection)
        then:
        1 * updateStationPreparedStatement.setString(1, expectedProtocol)
        1 * updateStationPreparedStatement.setString(2, KEY)
        1 * updateStationPreparedStatement.setString(3, ISP)
        1 * updateStationPreparedStatement.setString(4, PHONE)
        1 * updateStationPreparedStatement.setString(5, BSSID_VALUE.uncapitalize())
        1 * updateStationPreparedStatement.setString(6, SSID_VALUE)
        1 * updateStationPreparedStatement.execute()
        1 * dbConnection.close()
        2 * findStationPreparedStatement.close()
        where:
        wifiData                    || expectedProtocol             | description
        PROTECTED_NETWORK_WIFI_DATA || "$WPA_AUTH_TYPE $ENCRYPTION" | "protected network"
        OPEN_NETWORK_WIFI_DATA      || OPEN_AUTH_TYPE               | "open network"
    }

    @Unroll
    def "Test that a station is inserted for a #description"() {
        given:
        def findStationResult = Mock(ResultSet) {
            next() >> STATION_DOES_NOT_EXIST
        }
        def findStationPreparedStatement = Mock(PreparedStatement) {
            executeQuery() >> findStationResult
        }
        def updateStationPreparedStatement = Mock(PreparedStatement)

        def dbConnection = Mock(Connection) {
            prepareStatement(UpdaterDao.FIND_STATION_SQL_STATEMENT) >> findStationPreparedStatement
            prepareStatement(UpdaterDao.INSERT_STATION_SQL_STATEMENT) >> updateStationPreparedStatement
        }
        UpdaterDao.setIsp(ISP)
        when:
        UpdaterDao.saveStation(wifiData, HOST, dbConnection)
        then:
        1 * updateStationPreparedStatement.setString(1, BSSID_VALUE.uncapitalize())
        1 * updateStationPreparedStatement.setString(2, SSID_VALUE)
        1 * updateStationPreparedStatement.setString(3, expectedProtocol)
        1 * updateStationPreparedStatement.setString(4, KEY)
        1 * updateStationPreparedStatement.setString(5, ISP)
        1 * updateStationPreparedStatement.setString(6, PHONE)
        1 * updateStationPreparedStatement.execute()
        1 * dbConnection.close()
        2 * findStationPreparedStatement.close()
        where:
        wifiData                    || expectedProtocol             | description
        PROTECTED_NETWORK_WIFI_DATA || "$WPA_AUTH_TYPE $ENCRYPTION" | "protected network"
        OPEN_NETWORK_WIFI_DATA      || OPEN_AUTH_TYPE               | "open network"
    }
}
