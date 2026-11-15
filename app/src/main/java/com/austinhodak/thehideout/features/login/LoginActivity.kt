package com.austinhodak.thehideout.features.login

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Scaffold
import androidx.compose.ui.res.painterResource
import com.austinhodak.thehideout.GodActivity
import com.austinhodak.thehideout.R
import com.austinhodak.thehideout.ui.theme.HideoutTheme

class LoginActivity : GodActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HideoutTheme {
                Scaffold {
                    Column {
                        Image(painter = painterResource(id = R.drawable.bulb), contentDescription = "")
                        //Image(painter = painterResource(id = R.drawable.email_button), contentDescription = "", modifier = Modifier.width(200.dp))
                    }
                }
            }
        }
    }
}