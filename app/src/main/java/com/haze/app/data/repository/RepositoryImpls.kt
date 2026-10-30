package com.haze.app.data.repository

import com.haze.app.data.demo.DemoDataStore
import com.haze.app.data.local.DemoProfilePreferences
import com.haze.app.domain.model.LocalPromotion
import com.haze.app.domain.model.UserProfile
import com.haze.app.domain.model.Vehicle
import com.haze.app.domain.repository.MobilityRepository
import com.haze.app.domain.repository.UserRepository
import com.haze.app.domain.usecase.RouteMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class UserRepositoryImpl(
    private val preferences: DemoProfilePreferences
) : UserRepository {

    override fun getUserProfile(): Flow<UserProfile?> {
        return preferences.observeUserProfile()
    }

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            preferences.saveUserProfile(profile)
            // Asynchronously sync to Supabase (safe & non-blocking if offline/unconfigured)
            com.haze.app.data.remote.SupabaseClient.insertUser(profile.fullName, profile.phoneNumber)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearUserProfile(): Result<Unit> {
        return try {
            preferences.clearUserProfile()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return preferences.getUserProfile() != null
    }
}

class DemoMobilityRepositoryImpl(
    private val routeMatcher: RouteMatcher = RouteMatcher()
) : MobilityRepository {

    override fun getSimulatedVehicles(): Flow<List<Vehicle>> {
        return flowOf(DemoDataStore.demoVehicles)
    }

    override suspend fun getVehicleById(vehicleId: String): Vehicle? {
        return DemoDataStore.demoVehicles.find { it.vehicleId == vehicleId }
    }

    override suspend fun searchCompatibleVehicles(
        pickupQuery: String,
        dropQuery: String
    ): List<Vehicle> {
        return routeMatcher.findCompatibleVehicles(
            vehicles = DemoDataStore.demoVehicles,
            pickupQuery = pickupQuery,
            dropQuery = dropQuery
        )
    }

    override fun getLocalPromotions(): Flow<List<LocalPromotion>> {
        return flowOf(DemoDataStore.demoPromotions)
    }
}
