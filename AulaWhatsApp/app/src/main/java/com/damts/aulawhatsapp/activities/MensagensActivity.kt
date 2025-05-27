package com.damts.aulawhatsapp.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.damts.aulawhatsapp.R
import com.damts.aulawhatsapp.adapters.MensagensAdapter
import com.damts.aulawhatsapp.databinding.ActivityMensagensBinding
import com.damts.aulawhatsapp.model.Conversa
import com.damts.aulawhatsapp.model.Mensagem
import com.damts.aulawhatsapp.model.Usuario
import com.damts.aulawhatsapp.utils.Constantes
import com.damts.aulawhatsapp.utils.exibirMensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MensagensActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMensagensBinding.inflate(layoutInflater)
    }
    private val firebaseAuth by lazy{
        FirebaseAuth.getInstance()
    }
    private val firestore by lazy{
        FirebaseFirestore.getInstance()
    }
    private lateinit var listenerRegistration: ListenerRegistration
    private var dadosDestinatario: Usuario? = null
    private var dadosUsuarioRemetente: Usuario? = null
    private lateinit var conversasAdapter: MensagensAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        recuperarDadosUsuarios()
        inicializarToolbar()
        inicializarEventosClique()
        inicializarRecyclerView()
        inicializarListeners()
    }

    private fun inicializarRecyclerView() {

        with(binding){
            conversasAdapter = MensagensAdapter()
            rvMensagens.adapter =conversasAdapter
            rvMensagens.layoutManager = LinearLayoutManager(applicationContext)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration.remove()
    }

    private fun inicializarListeners() {

        val idUsuarioRemetente = firebaseAuth.currentUser?.uid
        val idUsuarioDestinatario = dadosDestinatario?.id
        if( idUsuarioRemetente != null && idUsuarioDestinatario != null ){

            listenerRegistration = firestore
                .collection(Constantes.MENSAGENS)
                .document( idUsuarioRemetente )
                .collection( idUsuarioDestinatario )
                .orderBy("data", Query.Direction.ASCENDING)
                .addSnapshotListener{ querySnaphot, erro ->

                    if ( erro != null ){
                        exibirMensagem("Erro ao recuperar mensagens")
                    }

                    val listaMensagens = mutableListOf<Mensagem>()
                    val documentos = querySnaphot?.documents

                    documentos?.forEach{ documentSnapshot ->

                        val mensagem = documentSnapshot.toObject( Mensagem::class.java )
                        if( mensagem != null ){
                            listaMensagens.add( mensagem )
                            Log.i("exibicao_mensagens", mensagem.mensagem)
                        }
                    }

                    //Lista
                    if( listaMensagens.isNotEmpty() ){
                        //Carregar os dados Adapter
                        conversasAdapter.adicionarLista( listaMensagens )
                    }


                }
        }

    }

    private fun inicializarEventosClique() {

        binding.fabEnviar.setOnClickListener {
            val mensagem = binding.editMensagem.text.toString()
            salvarMensagem( mensagem )
        }

    }

    private fun salvarMensagem( textoMensagem: String ) {

        if( textoMensagem.isNotEmpty() ){

            val idUsuarioRemetente = firebaseAuth.currentUser?.uid
            val idUsuarioDestinatario = dadosDestinatario?.id
            if( idUsuarioRemetente != null && idUsuarioDestinatario != null ){
                val mensagem = Mensagem(
                    idUsuarioRemetente, textoMensagem,
                )

                //Salvar para o Remetente
                salvarMensagemFirestore(
                    idUsuarioRemetente, idUsuarioDestinatario, mensagem
                )
                //Mateus -> Foto e nome Destinatario (Gabriela)
                val conversaRemetente = Conversa(
                    idUsuarioRemetente, idUsuarioDestinatario,
                    dadosDestinatario!!.foto, dadosDestinatario!!.nome,
                    textoMensagem
                )
                salvarConversaFirestore( conversaRemetente )

                //Salvar mesma mensagem para o Destinatario
                salvarMensagemFirestore(
                    idUsuarioDestinatario, idUsuarioRemetente, mensagem
                )
                //Gabriela -> Foto e nome Remetente (Mateus)
                val conversaDestinatario = Conversa(
                    idUsuarioDestinatario, idUsuarioRemetente,
                    dadosUsuarioRemetente!!.foto, dadosUsuarioRemetente!!.nome,
                    textoMensagem
                )
                salvarConversaFirestore( conversaDestinatario )

                binding.editMensagem.setText("")

            }
        }

    }

    private fun salvarConversaFirestore(conversa: Conversa) {

        firestore
            .collection(Constantes.CONVERSAS)
            .document( conversa.idUsuarioRemetente )
            .collection( Constantes.ULTIMAS_CONVERSAS )
            .document( conversa.idUsuarioDestinatario )
            .set( conversa )
            .addOnFailureListener {
                exibirMensagem("Erro ao salvar conversa")
            }
    }

    private fun salvarMensagemFirestore(
        idUsuarioRemetente: String,
        idUsuarioDestinatario: String,
        mensagem: Mensagem
    ) {
        firestore
            .collection(Constantes.MENSAGENS)
            .document( idUsuarioRemetente )
            .collection( idUsuarioDestinatario )
            .add( mensagem )
            .addOnFailureListener {
                exibirMensagem("Erro ao enviar mensagem")
            }

    }

    private fun recuperarDadosUsuarios() {

        //Dados do usuario logado
        val idUsuarioRemetente = firebaseAuth.currentUser?.uid
        if ( idUsuarioRemetente != null ) {
            firestore
                .collection(Constantes.USUARIOS)
                .document( idUsuarioRemetente )
                .get()
                .addOnSuccessListener { documentSnapshot ->

                    val usuario = documentSnapshot.toObject(Usuario::class.java)
                    if( usuario != null ){
                        dadosUsuarioRemetente = usuario

                    }
                }
        }

        //Recuperando dados destinatario
        val extras = intent.extras
        if( extras != null ){
            dadosDestinatario = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable("dadosDestinatario", Usuario::class.java)
            }else{
                extras.getParcelable("dadosDestinatario")
            }
        }

    }

    private fun inicializarToolbar() {
        val toolbar = binding.tbMensagens
        setSupportActionBar( toolbar )
        supportActionBar?.apply {
            title = ""
            if( dadosDestinatario != null ){
                binding.textNome.text = dadosDestinatario!!.nome
                //Desabilitado pois não tem DB configurado
                /*Picasso.get()
                    .load( dadosDestinatario!!.foto)
                    .into( binding.imageFotoPerfil )*/
            }
            setDisplayHomeAsUpEnabled(true)
        }

        addMenuProvider(
            object : MenuProvider{
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_mensagens, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        android.R.id.home -> {
                            finish()
                            true
                        }
                        R.id.item_excluir -> {
                            excluirConversa()
                            true
                        }else -> false
                    }
                }

            }
        )

    }

    private fun excluirConversa( ) {

        AlertDialog.Builder(this)
            .setTitle("Excluir")
            .setMessage("Deseja realmente excluir a conversa?")
            .setNegativeButton("Não") { dialog, posicao -> }
            .setPositiveButton("Sim") { dialog, posicao ->
                val idUsuarioRemetente = firebaseAuth.currentUser?.uid
                val idUsuarioDestinatario = dadosDestinatario?.id
                if (idUsuarioRemetente != null && idUsuarioDestinatario != null) {
                    // 🔥 1 - Excluir a conversa (da lista de últimas conversas)
                    firestore.collection(Constantes.CONVERSAS)
                        .document(idUsuarioRemetente)
                        .collection(Constantes.ULTIMAS_CONVERSAS)
                        .document(idUsuarioDestinatario)
                        .delete()
                        .addOnSuccessListener {
                            Log.i("FIREBASE", "Conversa excluída com sucesso")
                        }
                        .addOnFailureListener {
                            exibirMensagem("Erro ao excluir a conversa")
                        }

                    // 🔥 2 - Excluir todas as mensagens desse chat
                    val mensagensRef = firestore.collection(Constantes.MENSAGENS)
                        .document(idUsuarioRemetente)
                        .collection(idUsuarioDestinatario)

                    mensagensRef.get()
                        .addOnSuccessListener { querySnapshot ->
                            val batch = firestore.batch()

                            for (document in querySnapshot.documents) {
                                batch.delete(document.reference)
                            }

                            batch.commit()
                                .addOnSuccessListener {
                                    exibirMensagem("Conversa excluída com sucesso")
                                    finish() // Fecha a tela da conversa
                                }
                                .addOnFailureListener {
                                    exibirMensagem("Erro ao excluir mensagens")
                                }
                        }
                }
            }
            .show()
    }

}