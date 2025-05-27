package com.damts.aulawhatsapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.damts.aulawhatsapp.databinding.ItemContatosBinding
import com.damts.aulawhatsapp.model.Usuario
import com.squareup.picasso.Picasso

class ContatosAdapter(
    private val onClick: (Usuario) -> Unit
): Adapter<ContatosAdapter.ContatosViewHolder>() {

    private var listaContatos = emptyList<Usuario>()
    fun adicionarLista( lista: List<Usuario> ){
        listaContatos = lista
        notifyDataSetChanged()
    }

    inner class ContatosViewHolder(
        private val binding: ItemContatosBinding
    ): ViewHolder( binding.root ){

        fun bind( usuario: Usuario ){

            binding.textContatoNome.text = usuario.nome
            //Puxa a foto do banco de dados, desabilitado pois não tenho banco de dados
            /*Picasso.get()
                .load( usuario.foto )
                .into( binding.imageContatoFoto )*/

            //Evento de clique
            binding.clItemContato.setOnClickListener {
                onClick( usuario )
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContatosViewHolder {

        val inflater = LayoutInflater.from( parent.context )
        val itemView = ItemContatosBinding.inflate(
            inflater, parent, false
        )
        return ContatosViewHolder( itemView )

    }

    override fun onBindViewHolder(holder: ContatosViewHolder, position: Int) {
        val usuario = listaContatos[position]
        holder.bind( usuario )
    }

    override fun getItemCount(): Int {
        return  listaContatos.size
    }
}