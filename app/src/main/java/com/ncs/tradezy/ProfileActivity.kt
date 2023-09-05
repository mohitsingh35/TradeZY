package com.ncs.tradezy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.ncs.tradezy.repository.RealTimeUserResponse
import com.ncs.tradezy.googleAuth.GoogleAuthActivity
import com.ncs.marketplace.googleAuth.GoogleAuthUIClient
import com.ncs.tradezy.ui.theme.betterWhite
import com.ncs.tradezy.ui.theme.primary

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileActivity : ComponentActivity() {
    private val googleAuthUiClient by lazy {
        GoogleAuthUIClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        var token =""
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM token profile", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            token = task.result
        })
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: ProfileActivityViewModel = hiltViewModel()
            var userList=ArrayList<String>()
            val res=viewModel.res.value
            var isUserinDB by remember {
                mutableStateOf(false)
            }
            for (i in 0 until res.item.size){
                userList.add(res.item[i].item?.userId!!)
            }
            if (userList.contains(googleAuthUiClient.getSignedInUser()?.userID)){
                isUserinDB=true
            }

            ShowUserProfile(isUserinDB, token = token)

        }
    }
    private fun navigateToSignInActivity() {
        val intent = Intent(this, GoogleAuthActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ShowUserProfile(isUserinDB:Boolean,viewModel: ProfileActivityViewModel = hiltViewModel(),token:String){
        val scope= rememberCoroutineScope()
        val userData= googleAuthUiClient.getSignedInUser()
        val res=viewModel.res.value
        val currentUser=userData?.userID
        var filteredList:List<RealTimeUserResponse>?=null
        val isUpdate = remember {
            mutableStateOf(false)
        }
        val createNewUserinDb = remember {
            mutableStateOf(false)
        }
        if (isUserinDB) {
            filteredList = res.item.filter { userResponse ->
                userResponse.item?.userId == currentUser
            }
        }
        if (isUpdate.value){
            updateUser(isUpdate = isUpdate, itemState = filteredList?.get(0)!! , viewModel = viewModel, fcmToken = token  )
        }
        if (createNewUserinDb.value){
            CreateNewUserinDb(isUpdate = createNewUserinDb, viewModel = viewModel)
        }
        var udata= filteredList?.get(0)?.item
        if (isUserinDB){
            var username by remember {
                mutableStateOf(udata?.name)
            }
            var email by remember {
                mutableStateOf(udata?.email)
            }
            var phNum by remember {
                mutableStateOf(udata?.phNumber)
            }
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .background(primary),horizontalAlignment = Alignment.CenterHorizontally) {
                items(1){
                    ProfileScreenContent(profileUrl = userData?.profilePictureUrl, username = username, email = email, phNum = phNum ){
                        isUpdate.value=true
                    }
                }

            }
        }
        else{
            var username by remember {
                mutableStateOf(userData?.username)
            }
            var email by remember {
                mutableStateOf(userData?.email)
            }
            var phNum by remember {
                mutableStateOf(userData?.phNum)
            }
            Column {
                ProfileScreenContent(profileUrl = userData?.profilePictureUrl, username = username, email = email, phNum = phNum ){
                   createNewUserinDb.value=true
                }
            }


        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun ProfileScreenContent(profileUrl:String?,username:String?,email:String?,phNum:String?,viewModel: HomeScreenViewModel= hiltViewModel(),onClick:()->Unit,){
        val res=viewModel.res.value
        val userads=ArrayList<EachAdResponse>()
        for (i in 0 until res.item.size){
            if (res.item[i].item?.sellerId==FirebaseAuth.getInstance().currentUser?.uid){
                userads.add(res.item[i])
            }
        }
        val activeAds=ArrayList<EachAdResponse>()
        for (i in 0 until userads.size){
            if (userads[i].item?.sold=="false"){
                activeAds.add(userads[i])
            }
        }
        val soldAds=ArrayList<EachAdResponse>()
        for (i in 0 until userads.size){
            if (userads[i].item?.sold=="true"){
                soldAds.add(userads[i])
            }
        }

            Column(modifier = Modifier
                .fillMaxSize()
                .background(primary)
                .padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(model = profileUrl, contentDescription ="", modifier = Modifier
                    .size(80.dp)
                    .clip(
                        CircleShape
                    ) )
                Spacer(modifier = Modifier.height(35.dp))
                Text(text = username!!, color = betterWhite )
                Spacer(modifier = Modifier.height(15.dp))
                Text(text = email!! ,color = betterWhite)
                Spacer(modifier = Modifier.height(15.dp))
                Text(text = phNum!!,color = betterWhite )
                Spacer(modifier = Modifier.height(15.dp))
                Box(modifier = Modifier
                    .height(30.dp)
                    .width(100.dp)
                    .background(Color.Red)
                    .clickable {
                        lifecycleScope.launch {
                            googleAuthUiClient.signOut()
                            navigateToSignInActivity()
                        }
                    }, contentAlignment = Alignment.Center){
                    Row {
                        Image(imageVector = Icons.Filled.Clear, contentDescription = "")
                        Text(text = "SignOut", color = betterWhite)
                    }

                }
                Spacer(modifier = Modifier.height(50.dp))
                Button(onClick = { onClick() }) {
                    Text(text = "Update")
                }
                Column {
                    Spacer(modifier = Modifier.height(50.dp))
                    Text(text = "All Ads", color = betterWhite)
                    Spacer(modifier = Modifier.height(20.dp))
                    LazyRow(){
                        items(1){

                            for (i in 0 until userads.size){
                                eachAd(item = userads[i])
                            }

                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = "Active Ads", color = betterWhite)
                    Spacer(modifier = Modifier.height(20.dp))
                    LazyRow(){
                        items(1){
                            for (i in 0 until activeAds.size){
                                eachAd(item = activeAds[i])
                            }

                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = "Sold Ads", color = betterWhite)
                    Spacer(modifier = Modifier.height(20.dp))
                    LazyRow(){
                        items(1){
                            for (i in 0 until soldAds.size){
                                eachAd(item = soldAds[i])
                            }

                        }
                    }
                }

            }

    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun updateUser(
        isUpdate: MutableState<Boolean>,
        itemState: RealTimeUserResponse,
        viewModel: ProfileActivityViewModel,
        fcmToken:String,
        ){
        val username= remember {
            mutableStateOf(itemState.item?.name)
        }
        val email= remember {
            mutableStateOf(itemState.item?.email)
        }
        val phNum= remember {
            mutableStateOf(itemState.item?.phNumber)
        }


        val scope= rememberCoroutineScope()
        val context= LocalContext.current
        if(isUpdate.value){
            AlertDialog(onDismissRequest = { isUpdate.value=false }, confirmButton = {
                Button(onClick = { scope.launch(Dispatchers.Main) {
                    viewModel.update(
                        RealTimeUserResponse(item = RealTimeUserResponse.RealTimeUsers(name=username.value, email = email.value, phNumber = phNum.value, fcmToken = fcmToken),key = itemState.key)
                    ).collect{
                        when(it){
                            is ResultState.Success->{
                                context.showMsg(
                                    msg=it.data
                                )
                                isUpdate.value=false
                                recreateActivity()

                            }
                            is ResultState.Failure->{
                                context.showMsg(
                                    msg=it.msg.toString()
                                )
                            }
                            ResultState.Loading->{
                            }
                        }
                    }
                }}) {
                    Text(text = "Update")
                }
            }, text = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center){
                        Text(text = "Update", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    username.value?.let {
                        TextField(value = it, onValueChange ={username.value=it}, label = { Text(
                            text = "Name"
                        )} )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    email.value?.let {
                        TextField(value = it, onValueChange ={email.value=it}, label = { Text(
                            text = "Email"
                        )} )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    phNum.value?.let {
                        TextField(value = it, onValueChange ={phNum.value=it}, label = { Text(
                            text = "Phone Number"
                        )} )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            )
        }

    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CreateNewUserinDb(
        isUpdate: MutableState<Boolean>,
        viewModel: ProfileActivityViewModel,

        ){
        val data=googleAuthUiClient.getSignedInUser()
        val username= remember {
            mutableStateOf(data?.username)
        }
        val email= remember {
            mutableStateOf(data?.email)
        }
        val phNum= remember {
            mutableStateOf(data?.phNum)
        }


        val scope= rememberCoroutineScope()
        val context= LocalContext.current
        if(isUpdate.value){
            AlertDialog(onDismissRequest = { isUpdate.value=false }, confirmButton = {
                Button(onClick = { scope.launch(Dispatchers.Main) {
                    viewModel.insertUser(
                        RealTimeUserResponse.RealTimeUsers
                            (userId = data?.userID,name = username.value,phNumber = phNum.value,profileDPurl = data?.profilePictureUrl,email = email.value)).collect {
                        when (it) {
                            is ResultState.Success -> {
                                context.showMsg(
                                    msg = it.data
                                )
                                isUpdate.value=false
                                recreateActivity()
                            }

                            is ResultState.Failure -> {
                                context.showMsg(
                                    msg = it.msg.toString()
                                )
                            }

                            ResultState.Loading -> {
                            }
                        }
                    }
                }}) {
                    Text(text = "Update")
                }
            }, text = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center){
                        Text(text = "Update", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    username.value?.let {
                        TextField(value = it, onValueChange ={username.value=it}, label = { Text(
                            text = "Name"
                        )} )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    email.value?.let {
                        TextField(value = it, onValueChange ={email.value=it}, label = { Text(
                            text = "Email"
                        )} )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    phNum.value?.let {
                        TextField(value = it, onValueChange ={phNum.value=it}, label = { Text(
                            text = "Phone Number"
                        )} )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            )
        }

    }
    private fun recreateActivity() {
        val intent = Intent(this, ProfileActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

}



