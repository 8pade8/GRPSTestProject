package server

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.stub.StreamObserver
import server.db.DbHandler
import server.model.Profile
import test.*

val dbHandler = DbHandler.instance

class GRPCServer private constructor(
    val port: Int,
    val server: Server
) {
    constructor(
        port: Int
    ) : this(
        port = port,
        server = ServerBuilder.forPort(port).addService(ProfileService()).build()
    )

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@GRPCServer.stop()
                println("*** server shut down")
            }
        )
    }

    fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    class ProfileService() : ProfileServiceGrpc.ProfileServiceImplBase() {

        override fun createProfile(
            request: CreateProfileRequest?,
            responseObserver: StreamObserver<CreateProfileResponse>?
        ) {
            println("CreateProfileRequest")
            val profile: Profile? = dbHandler?.addProfile()
            if (profile != null) {
                CreateProfileResponse.newBuilder()
                    .setProfileId(profile.profileId)
                    .setSecretKey(profile.secretKey)
                    .setMoney(profile.amount)
                    .build().also {
                        responseObserver?.onNext(it)
                        responseObserver?.onCompleted()
                        println("CreateProfileResponse: profileID = ${it.profileId}, secretKey = ${it.secretKey}, money=${it.money}")
                    }
            }
        }

        override fun getProfile(request: GetProfileRequest?, responseObserver: StreamObserver<GetProfileResponse>?) {
            print("GetProfileRequest: secretKey = ${request?.secretKey}")
            request?.secretKey?.let {
                if (!checkSecretKey(it)) {
                    responseObserver?.onError(StatusException(Status.NOT_FOUND))
                    responseObserver?.onCompleted()
                    println("GetProfileResponse: user not found")
                }
                val profile = dbHandler?.getProfile(it)
                if (profile != null) {
                    GetProfileResponse.newBuilder()
                        .setProfileId(profile.profileId)
                        .setMoney(profile.amount)
                        .build().also {
                            responseObserver?.onNext(it)
                            responseObserver?.onCompleted()
                            println("GetProfileResponse: profileID = ${it.profileId}, money=${it.money}")
                        }
                }
            }
        }

        override fun addMoney(request: AddMoneyRequest?, responseObserver: StreamObserver<AddMoneyResponse>?) {
            println("AddMoneyRequest: secretKey = ${request?.secretKey}, amountToAdd = ${request?.amountToAdd}")
            if (request != null) {
                if (!checkSecretKey(request.secretKey)) {
                    responseObserver?.onError(StatusException(Status.NOT_FOUND))
                    responseObserver?.onCompleted()
                    println("AddMoneyResponse: user not found")
                }
                val profile = dbHandler?.getProfile(request.secretKey)
                if (profile != null) {
                    val currentAmount = profile.amount
                    val newAmount = currentAmount + request.amountToAdd
                    dbHandler.updateAmount(request.secretKey, newAmount)
                    val newProfile = dbHandler.getProfile(request.secretKey)
                    if (newProfile != null) {
                        AddMoneyResponse.newBuilder()
                            .setCurrentAmount(newProfile.amount)
                            .build().also {
                                responseObserver?.onNext(it)
                                responseObserver?.onCompleted()
                                println("AddMoneyResponse: money=${it.currentAmount}")
                            }
                    }
                }
            }
        }

        override fun chargeMoney(request: ChargeMoneyRequest?, responseObserver: StreamObserver<ChargeMoneyResponse>?) {
            println("ChargeMoneyRequest: secretKey = ${request?.secretKey}, amountToCharge = ${request?.amountToCharge}")
            if (request != null) {
                if (!checkSecretKey(request.secretKey)) {
                    responseObserver?.onError(StatusException(Status.NOT_FOUND))
                    responseObserver?.onCompleted()
                    println("ChargeMoneyResponse: user not found")
                }
                val profile = dbHandler?.getProfile(request.secretKey)
                if (profile != null) {
                    val currentAmount = profile.amount
                    if (currentAmount - request.amountToCharge < 0) {
                        ChargeMoneyResponse.newBuilder()
                            .setError("Недостаточно средств")
                            .build().also {
                                responseObserver?.onNext(it)
                                responseObserver?.onCompleted()
                                println("ChargeMoneyResponse: Error \"Недостаточно средств\"")
                            }
                    } else {
                        val newAmount = currentAmount - request.amountToCharge
                        dbHandler.updateAmount(request.secretKey, newAmount)
                        val newProfile = dbHandler.getProfile(request.secretKey)
                        if (newProfile != null) {
                            ChargeMoneyResponse.newBuilder()
                                .setCurrentAmount(newProfile.amount)
                                .build().also {
                                    responseObserver?.onNext(it)
                                    responseObserver?.onCompleted()
                                    println("ChargeMoneyResponse: money=${it.currentAmount}")
                                }
                        }
                    }
                }
            }
        }

        private fun checkSecretKey(secretKey:String): Boolean = dbHandler?.existSecretKey(secretKey) ?: false
    }
}

fun main() {
    val port = 8080
    val server = GRPCServer(port)
    server.start()
    server.blockUntilShutdown()
}