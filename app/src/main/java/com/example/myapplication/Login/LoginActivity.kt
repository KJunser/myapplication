package com.example.myapplication.Login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.myapplication.DTO.UserinfoDTO
import com.example.myapplication.Main.Activity.MainActivity
import com.example.myapplication.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.AuthErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class LoginActivity : AppCompatActivity() {

    // Firebase 인증 객체 생성
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private val TAG = "LoginActivity"

    lateinit var login_id: String
    lateinit var login_pw: String

    private lateinit var googleSignInclient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 파이어베이스 인증 객체 선언
        auth = FirebaseAuth.getInstance()

        /*
        //ViewPager 전용 List (메인 장식 화면)
        val listImage = ArrayList<Int>()
        listImage.add(R.drawable.이미지 파일 명)
        listImage.add(R.drawable.ic_launcher_foreground)

        //ViewPager Adapter 등록
        val loginimagefragmentAdapter = LogInImageFragmentAdapter(supportFragmentManager)
        login_viewPager.adapter = loginimagefragmentAdapter

        (미완)
        */
        // Google 로그인 환경설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString((R.string.default_web_client_id)))
            .build()

        googleSignInclient = GoogleSignIn.getClient(this, gso)
        database = Firebase.database.reference

        bt_login.setOnClickListener {
            signIn()
        }
        bt_signup.setOnClickListener {
            signUp()
        }
        signin_googleButton.setOnClickListener {
            google_signIn()
        }

        // 로그인이 됐다면 카카오 자동 로그인(로그인 유지)
        UserApiClient.instance.accessTokenInfo { tokenInfo, error ->
            if (error != null) {
                Toast.makeText(this, "토튼 정보 보기 실패", Toast.LENGTH_SHORT).show()
            } else if (tokenInfo != null) {
                Toast.makeText(this, "토큰 정보 보기 성공", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
        }
        // 카카오 로그인 에러
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                when {
                    error.toString() == AuthErrorCause.AccessDenied.toString() -> {
                        Toast.makeText(this, "접근이 거부 된(동의 취소)", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.InvalidClient.toString() -> {
                        Toast.makeText(this, "유효하지 않은 앱", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.InvalidGrant.toString() -> {
                        Toast.makeText(this, "인증 수단이 유효하지 않아 인증할 수 없는 상태", Toast.LENGTH_SHORT)
                            .show()
                    }
                    error.toString() == AuthErrorCause.InvalidRequest.toString() -> {
                        Toast.makeText(this, "요청 파라미터 오류", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.InvalidScope.toString() -> {
                        Toast.makeText(this, "유효하지 않은 scope ID", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.Misconfigured.toString() -> {
                        Toast.makeText(this, "설정이 올바르지 않음(android key hash", Toast.LENGTH_SHORT)
                            .show()
                    }
                    error.toString() == AuthErrorCause.ServerError.toString() -> {
                        Toast.makeText(this, "서버 내부 에러", Toast.LENGTH_SHORT).show()
                    }
                    error.toString() == AuthErrorCause.Unauthorized.toString() -> {
                        Toast.makeText(this, "앱이 요청 권한이 없음", Toast.LENGTH_SHORT).show()
                    }
                    else -> { //Unknown
                        Toast.makeText(this, "기타 에러", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (token != null) {
                Toast.makeText(this, "로그인에 성공하였습니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
        }
        // 카카오톡이 설치되어 있으면 카카오톡으로 로그인 아니면 카카오 계정으로 로그인
        kakao_login_button.setOnClickListener {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                UserApiClient.instance.loginWithKakaoTalk(this, callback = callback)
            } else {
                UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
            }
        }

    }

    //자동 로그인
    override fun onStart() {
        super.onStart()
        var currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    // 텍스트 객체에서 받아온 파라미터가 있는지 없는지 검사
    fun isValidId(): Boolean {
        if (login_id.isEmpty())
            return false
        else
            return true
    }

    fun isValidPw(): Boolean {
        if (login_pw.isEmpty())
            return false
        else
            return true
    }

    //일반 로그인
    fun signIn() {
        login_id = login_Id.text.toString()
        login_pw = login_Pw.text.toString()
        if (isValidId() && isValidPw())
            loginrUser(login_id, login_pw)
    }

    //회원 가입
    fun signUp() {
        login_id = login_Id.text.toString()
        login_pw = login_Pw.text.toString()
        if (isValidId() && isValidPw())
            createUser(login_id, login_pw)
    }

    // 구글 로그인 처리
    fun google_signIn() {
        val signInIntent = googleSignInclient.signInIntent
        startActivityForResult(signInIntent, 100)
    }

    // 일반 로그인

    fun loginrUser(login_id: String, login_pw: String) {
        auth.signInWithEmailAndPassword(login_id, login_pw)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "로그인 성공입니다.", Toast.LENGTH_SHORT).show()
                    startActivity(intent)
                } else
                    Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
            }
    }

    //회원가입
    fun createUser(login_id: String, login_pw: String) {
        if (login_Id.text.toString().length == 0 || login_Pw.text.toString().length == 0) {
            Toast.makeText(this, "email 혹은 페스워드를 반드시 입력하세요,", Toast.LENGTH_SHORT).show()
        } else {
            auth.createUserWithEmailAndPassword(login_id, login_pw)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "환영합니다", Toast.LENGTH_SHORT).show()

                        // 회원가입이 성공하면 firestore email 및 uid 저장
                        val userInfoDTO = UserinfoDTO()
                        var uemail = FirebaseAuth.getInstance().currentUser!!.email
                        var uid = FirebaseAuth.getInstance().currentUser!!.uid

                        userInfoDTO.userEmail = uemail.toString()
                        userInfoDTO.userId = uid
                        userInfoDTO.signUpdate = SimpleDateFormat("yyyyMMdd").format(Date())

                        database.child("userid").child(uid).setValue(userInfoDTO)
                        //파이어스토어
//                        FirebaseFirestore.getInstance().collection("userid").document("uid")
//                            .set(userInfoDTO)
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(this, "회원가입 실패", Toast.LENGTH_SHORT).show()
                        //입력필드 초기화
                        login_Id?.setText("")
                        login_Pw?.setText("")
                        login_Id.requestFocus()
                    }
                }

        }
    }
}

