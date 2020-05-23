package server.db

import org.sqlite.JDBC
import server.model.Profile
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import javax.crypto.KeyGenerator


class DbHandler private constructor() {

    private val connection: Connection

    companion object {
        private const val CON_STR = "jdbc:sqlite:src/main/kotlin/server/db/profiles.db"
        @get:Throws(SQLException::class)
        @get:Synchronized
        var instance: DbHandler? = null
            get() {
                if (field == null) field = DbHandler()
                return field
            }
            private set
    }

    init {
        DriverManager.registerDriver(JDBC())
        connection = DriverManager.getConnection(CON_STR)
    }

     fun addProfile():Profile? {
        val keyGenerator = KeyGenerator.getInstance("AES")
        val secretKey = Base64.getEncoder().encodeToString(keyGenerator.generateKey().encoded)

        try {
            connection.prepareStatement(
                "INSERT INTO Profiles(`secret_key`, `amount`) " +
                        "VALUES(?, ?)"
            ).use { statement ->
                statement.setObject(1, secretKey)
                statement.setObject(2, 0)
                // Выполняем запрос
                statement.execute()
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return getProfile(secretKey)
    }

    fun getProfile(secretKey: String) : Profile?{
        try {
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery("SELECT profile_id, secret_key, amount FROM profiles WHERE secret_key = \"$secretKey\"")
                resultSet.next()
                 return Profile(
                    profileId = resultSet.getInt("profile_id").toLong(),
                     secretKey = resultSet.getString("secret_key"),
                     amount = resultSet.getInt("amount").toLong()
                )
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return null;
        }
    }

    fun updateAmount(secretKey: String, amount: Long) : Profile? {
        try {
            connection.prepareStatement(
                "UPDATE profiles SET amount = $amount WHERE secret_key = \"$secretKey\""
            ).use { statement ->
                statement.execute()
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return getProfile(secretKey)
    }

    fun existSecretKey(secretKey: String): Boolean {
        try {
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery("SELECT secret_key FROM profiles WHERE secret_key = \"$secretKey\"")
                return resultSet.next()
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return false;
        }
    }
}