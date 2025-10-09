package com.android.swisstravel.model.map

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.swent.swisstravel.model.map.PermissionHandler
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class PermissionHandlerTest {

    private lateinit var activity: Activity
    private lateinit var permissionHandler: PermissionHandler

    @Before
    fun setUp() {
        activity = mockk(relaxed = true)
        permissionHandler = PermissionHandler(activity)
        mockkStatic(ContextCompat::class)
        mockkStatic(ActivityCompat::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // --- arePermissionsGranted() ---

    @Test
    fun `arePermissionsGranted returns true when permission is granted`() {
        // Arrange
        every {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } returns PackageManager.PERMISSION_GRANTED

        // Act
        val result = permissionHandler.arePermissionsGranted()

        // Assert
        assertTrue(result)
        verify(exactly = 1) {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @Test
    fun `arePermissionsGranted returns false when permission is denied`() {
        // Arrange
        every {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } returns PackageManager.PERMISSION_DENIED

        // Act
        val result = permissionHandler.arePermissionsGranted()

        // Assert
        assertFalse(result)
        verify(exactly = 1) {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // --- requestPermission() ---

    @Test
    fun `requestPermission calls ActivityCompat requestPermissions with correct arguments`() {
        // Arrange
        val requestCode = 42
        every {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                requestCode
            )
        } just Runs

        // Act
        permissionHandler.requestPermission(requestCode)

        // Assert
        verify(exactly = 1) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                requestCode
            )
        }
    }
}