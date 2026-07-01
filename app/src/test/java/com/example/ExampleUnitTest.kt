package com.example

import com.example.network.GeminiClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testProfileAnalysisFallback() = runBlocking {
    val bio = "Hi, I like coffee."
    val interests = "Coffee"
    val result = GeminiClient.analyzeProfile(bio, interests)
    
    assertTrue(result.contains("AI Profile Optimization Audit"))
    assertTrue(result.contains("Expand your bio"))
    assertTrue(result.contains("Add more interests"))
    assertTrue(result.contains("Suggested Revised Bio"))
  }
}
