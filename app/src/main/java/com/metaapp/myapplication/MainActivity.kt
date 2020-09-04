package com.metaapp.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startAnimationBt.setOnClickListener {
            mainBombView.init(
                90, 0.5f, 0.5f, 0.5f, 500L, CommonDef.mDrawableResIds
            )
            mainBombView.startBomb()
        }
    }

}