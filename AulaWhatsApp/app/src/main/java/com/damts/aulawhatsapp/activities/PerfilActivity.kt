package com.damts.aulawhatsapp.activities

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.damts.aulawhatsapp.databinding.ActivityPerfilBinding
import com.damts.aulawhatsapp.utils.exibirMensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class PerfilActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityPerfilBinding.inflate(layoutInflater)
    }

    private val firebaseAuth by lazy{
        FirebaseAuth.getInstance()
    }

    private val storage by lazy{
        FirebaseStorage.getInstance()
    }

    private val firestore by lazy{
        FirebaseFirestore.getInstance()
    }

    private var temPermissaoCamera = false
    private var temPermissaoGaleria = false

    private val gerenciadorGaleria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ){uri ->
        if( uri != null ){
            binding.imagePerfil.setImageURI( uri )
            uploadImagemStorage(uri)
        }else{
            exibirMensagem("Nenhuma imagem selecionada")
        }
    }

    //    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        inicializarToolbar()
        solicitarPermissoes() //Permissao funciona normalmente
        //Começa problema permissao
        inicializarEventosClique()

    }

    override fun onStart() {
        super.onStart()
        recuperarDadosIniciasUsuario()
    }

    private fun recuperarDadosIniciasUsuario() {
        val idUsuario = firebaseAuth.currentUser?.uid
        if ( idUsuario != null){
            firestore
                .collection("usuarios")
                .document( idUsuario )
//                .addSnapshotListener() // Para continuar recuperando os dados a cada mudança
                .get()
                .addOnSuccessListener {documentSnapshot ->

                    val dadosUsuario = documentSnapshot.data
                    if( dadosUsuario != null){

                        val nome = dadosUsuario["nome"] as String
                        val foto = dadosUsuario["foto"] as String

                        binding.editNomePerfil.setText( nome )
                        if( foto.isNotEmpty() ){
                            Picasso.get()
                                .load( foto )
                                .into( binding.imagePerfil )
                        }
                    }
                }
        }

    }

    private fun uploadImagemStorage(uri: Uri) {

        val idUsuario = firebaseAuth.currentUser?.uid
        if(idUsuario != null){
            storage
                .getReference("fotos")
                .child("usuarios")
                .child(idUsuario)
                .child("perfil.jpg")
                .putFile( uri )
                .addOnSuccessListener { task ->
                    exibirMensagem("Sucesso ao fazer upload da imagem")
                    task.metadata?.reference?.downloadUrl
                        ?.addOnSuccessListener {url ->

                            val dados = mapOf(
                                "foto" to url.toString()
                            )
                            atualizarDadosPerfil( idUsuario, dados )
                        }

                }
                .addOnFailureListener{
                    exibirMensagem("Erro ao fazer upload da imagem")
                }
        }
    }

    private fun atualizarDadosPerfil(idUsuario: String, dados: Map<String, String>) {

        firestore
            .collection("usuarios")
            .document( idUsuario )
            .update( dados )
            .addOnSuccessListener {
                exibirMensagem("Sucesso ao atualizar o perfil")
            }
            .addOnFailureListener {
                exibirMensagem("Erro ao atualizar o perfil")
            }
    }

    //    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun inicializarEventosClique() {

        binding.fabSelecionar.setOnClickListener {
            if( temPermissaoGaleria ){
                gerenciadorGaleria.launch("image/*")
            }else{
                exibirMensagem("Não tem permissão para acessar galeria")
                solicitarPermissoes()
            }
        }

        binding.btnAtualizarPerfil.setOnClickListener {

            val nomeUsuario = binding.editNomePerfil.text.toString()
            if( nomeUsuario.isNotEmpty() ){

                val idUsuario = firebaseAuth.currentUser?.uid
                if( idUsuario != null){
                    val dados = mapOf(
                        "nome" to nomeUsuario
                    )
                    atualizarDadosPerfil( idUsuario, dados )
                }

            }else{
                exibirMensagem("Preenca o nome para atualizar")
            }

        }

    }

//    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun solicitarPermissoes() {

        //Verifico se usuario ja tem permissão
        temPermissaoCamera = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )== PackageManager.PERMISSION_GRANTED

        temPermissaoGaleria = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_IMAGES
        )== PackageManager.PERMISSION_GRANTED

        //LISTA DE PERMISSÕES NEGADAS
        val listaPermissoesNegadas = mutableListOf<String>()
        if( !temPermissaoCamera )
            listaPermissoesNegadas.add ( Manifest.permission.CAMERA )
        if( !temPermissaoGaleria )
            listaPermissoesNegadas.add ( Manifest.permission.READ_MEDIA_IMAGES )

        if ( listaPermissoesNegadas.isNotEmpty() ){

            //Solicitar multiplas permissões
            val gerenciadorPermissoes = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ){permissoes ->

                temPermissaoCamera = permissoes[Manifest.permission.CAMERA]
                    ?: temPermissaoCamera

                temPermissaoGaleria = permissoes[Manifest.permission.READ_MEDIA_IMAGES]
                    ?: temPermissaoGaleria

            }
            gerenciadorPermissoes.launch( listaPermissoesNegadas.toTypedArray() )

        }
    }

    private fun inicializarToolbar() {
        val toolbar = binding.includeToolbarPerfil.tbPrincipal
        setSupportActionBar( toolbar )
        supportActionBar?.apply {
            title = "Editar Perfil"
            setDisplayHomeAsUpEnabled(true)
        }
    }

}