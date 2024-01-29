package com.fixeam.icoserkt

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.lang.IllegalStateException
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContentProviderCompat.requireContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentTheme == Configuration.UI_MODE_NIGHT_YES) {
            setStatusBarColor(Color.BLACK)
            setStatusBarTextColor(false)
        } else {
            setStatusBarColor(Color.WHITE)
            setStatusBarTextColor(true)
        }

        var toggleButton = findViewById<MaterialButtonToggleGroup>(R.id.tab_bar)
        toggleButton.addOnButtonCheckedListener{group, checkedId, isChecked ->
            if(!isChecked){
                return@addOnButtonCheckedListener
            }

            val childCount = group.childCount
            var selectIndex = 0
            val colorSelected = ContextCompat.getColor(this, R.color.brand_primary)

            for (index in 0 until childCount){
                val childAt = group.getChildAt(index) as MaterialButton
                if(childAt.id == checkedId){
                    selectIndex = index
                    childAt.setTextColor(colorSelected)
                    childAt.iconTint = ColorStateList.valueOf(colorSelected)
                } else {
                    if (currentTheme == Configuration.UI_MODE_NIGHT_YES) {
                        childAt.setTextColor(Color.WHITE)
                        childAt.iconTint = ColorStateList.valueOf(Color.WHITE)
                    } else {
                        childAt.setTextColor(Color.BLACK)
                        childAt.iconTint = ColorStateList.valueOf(Color.BLACK)
                    }
                }
            }

            switchFragment(selectIndex)
        }
        toggleButton.check(R.id.home_button)

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        val isConnected = activeNetworkInfo?.isConnectedOrConnecting == true

        if(!isConnected){
            Toast.makeText(this, "网络连接失败, 请检查您的网络和应用权限配置", Toast.LENGTH_SHORT).show()
        } else {
            val networkType = activeNetworkInfo?.type
            if (networkType == ConnectivityManager.TYPE_WIFI) {
                // 当前连接为 Wi-Fi
                Toast.makeText(this, "当前为WIFI环境，可放心浏览本APP", Toast.LENGTH_SHORT).show()
            } else if (networkType == ConnectivityManager.TYPE_MOBILE) {
                // 当前连接为移动网络
                Toast.makeText(this, "当前为流量环境，APP加载资源较多，请注意您的流量消耗", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var homeFragment: HomeFragment? = null
    private var smartVideoFragment: SmartVideoFragment? = null
    private var searchFragment: SearchFragment? = null
    private var userFragment:   UserFragment? = null
    private var showFragment: Fragment? = null

    private fun switchFragment(selectIndex: Int){
        val fragment = when(selectIndex){
            0->{
                if(homeFragment == null){
                    homeFragment = HomeFragment()
                }
                homeFragment
            }
            1->{
                if(smartVideoFragment == null){
                    smartVideoFragment = SmartVideoFragment()
                }
                smartVideoFragment
            }
            2->{
                if(searchFragment == null){
                    searchFragment = SearchFragment()
                }
                searchFragment
            }
            3->{
                if(userFragment == null){
                    userFragment = UserFragment()
                }
                userFragment
            }
            else -> {
                throw IllegalStateException("下标不符合预期")
            }
        } ?: return

        val ft = supportFragmentManager.beginTransaction()
        if(!fragment.isAdded){
            ft.add(R.id.container, fragment)
        }
        ft.show(fragment)
        if(showFragment != null){
            ft.hide(showFragment!!)
        }
        showFragment = fragment
        ft.commitAllowingStateLoss()
    }

    private fun setStatusBarColor(color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = color
        }
    }

    private fun setStatusBarTextColor(isDark: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = window.decorView
            var flags = decorView.systemUiVisibility
            if (isDark) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            decorView.systemUiVisibility = flags
        }
    }

}