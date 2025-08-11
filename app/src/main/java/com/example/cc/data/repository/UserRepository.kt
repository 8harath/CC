package com.example.cc.data.repository

import com.example.cc.data.dao.UserDao
import com.example.cc.data.model.User
import com.example.cc.data.model.UserRole
import kotlinx.coroutines.flow.Flow
class UserRepository(
    private val userDao: UserDao
) {
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()
    
    suspend fun getUserById(userId: Long): User? = userDao.getUserById(userId)
    
    fun getUsersByRole(role: UserRole): Flow<List<User>> = userDao.getUsersByRole(role)
    
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    
    suspend fun deleteUser(user: User) = userDao.deleteUser(user)
    
    suspend fun deleteAllUsers() = userDao.deleteAllUsers()
} 