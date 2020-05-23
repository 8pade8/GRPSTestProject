package client

import io.grpc.ManagedChannelBuilder
import test.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.NumberFormatException


fun main() {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    while (true) {
        println(
            "Используйте:\n" +
                    "create profile - для создания нового профиля\n" +
                    "get profile - для получения информации о профиле\nadd money для увелечения средств на балансе\n" +
                    "charge money - для списания средств\n" +
                    "exit - для выхода"
        )
        val input = reader.readLine().trim()
        when (input) {
            "exit" -> return
            "create profile" -> createProfile()
            "get profile" -> getProfile()
            "add money" -> addMoney()
            "charge money" -> chargeMoney()
        }
    }
}

fun createProfile() {
    val channel = ManagedChannelBuilder.forAddress("localhost", 8080)
        .usePlaintext()
        .build()
    val stub: ProfileServiceGrpc.ProfileServiceBlockingStub = ProfileServiceGrpc.newBlockingStub(channel)

    val response: CreateProfileResponse = stub.createProfile(CreateProfileRequest.newBuilder().build())

    println("id = ${response.profileId}, secretKey = ${response.secretKey}, amount = ${response.money}")

    channel.shutdown()
}

fun getProfile() {
    println("Введите секретный ключ (cancel - для отмены запроса):")
    val reader = BufferedReader(InputStreamReader(System.`in`))
    val input = reader.readLine().trim()
    if (input == "cancel") return
    val channel = ManagedChannelBuilder.forAddress("localhost", 8080)
        .usePlaintext()
        .build()
    val stub: ProfileServiceGrpc.ProfileServiceBlockingStub = ProfileServiceGrpc.newBlockingStub(channel)

    val response: GetProfileResponse = stub.getProfile(
        GetProfileRequest.newBuilder()
            .setSecretKey(input)
            .build()
    )

    println("id = ${response.profileId}, amount = ${response.money}")
    channel.shutdown()
}

fun addMoney() {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    println("Введите секретный ключ (cancel - для отмены запроса):")
    var input = reader.readLine().trim()
    if (input == "cancel") return
    val secretKey = input
    var addAmount: Long
    println("Введите сумму (cancel - для отмены запроса):")
    while (true) {
        try {
            input = reader.readLine().trim()
            if (input == "cancel") return
            addAmount = input.toLong()
            break
        } catch (ex: NumberFormatException) {
            println("Введите сумму, число! (cancel - для отмены запроса):")
        }
    }
    val channel = ManagedChannelBuilder.forAddress("localhost", 8080)
        .usePlaintext()
        .build()
    val stub: ProfileServiceGrpc.ProfileServiceBlockingStub = ProfileServiceGrpc.newBlockingStub(channel)

    val response: AddMoneyResponse = stub.addMoney(
        AddMoneyRequest.newBuilder()
            .setSecretKey(secretKey)
            .setAmountToAdd(addAmount)
            .build()
    )
    println("current amount = ${response.currentAmount}")
    channel.shutdown()
}

fun chargeMoney() {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    println("Введите секретный ключ (cancel - для отмены запроса):")
    var input = reader.readLine().trim()
    if (input == "cancel") return
    val secretKey = input
    var amountToCharge: Long
    println("Введите сумму (cancel - для отмены запроса):")
    while (true) {
        try {
            input = reader.readLine().trim()
            if (input == "cancel") return
            amountToCharge = input.toLong()
            break
        } catch (ex: NumberFormatException) {
            println("Введите сумму, число! (cancel - для отмены запроса):")
        }
    }
    val channel = ManagedChannelBuilder.forAddress("localhost", 8080)
        .usePlaintext()
        .build()
    val stub: ProfileServiceGrpc.ProfileServiceBlockingStub = ProfileServiceGrpc.newBlockingStub(channel)

    val response: ChargeMoneyResponse = stub.chargeMoney(
        ChargeMoneyRequest.newBuilder()
            .setSecretKey(secretKey)
            .setAmountToCharge(amountToCharge)
            .build()
    )

    if (response.error.isNullOrEmpty()) {
        println("current amount = ${response.currentAmount}")
    } else println(response.error)

    channel.shutdown()
}