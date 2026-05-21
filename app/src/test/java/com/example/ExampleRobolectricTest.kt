package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Qboard", appName)
  }

  @Test
  fun `test ComposeIME lifecycle and view creation`() {
    val controller = Robolectric.buildService(com.example.keyboard.ComposeIME::class.java)
    val service = controller.create().get()
    
    // Simulate window shown which triggers onWindowShown()
    service.onWindowShown()
    
    val inputView = service.onCreateInputView()
    assertNotNull(inputView)
    
    service.onWindowHidden()
    controller.destroy()
  }
}
