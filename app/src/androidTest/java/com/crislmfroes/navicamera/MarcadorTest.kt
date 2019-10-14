package com.crislmfroes.navicamera

import androidx.test.runner.AndroidJUnit4
import com.crislmfroes.navicamera.model.Marcador
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarcadorTest {
    @Test
    fun equalWorks() {
        val m1 = Marcador("m1", "qqcoisa", 1)
        val m2 = Marcador("m1", "qqcoisa", 1)
        Assert.assertEquals(m1, m2)
    }
}