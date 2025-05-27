package com.damts.aulawhatsapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.damts.aulawhatsapp.databinding.ItemContatosBinding
import com.damts.aulawhatsapp.databinding.ItemConversasBinding
import com.damts.aulawhatsapp.model.Conversa
import com.damts.aulawhatsapp.model.Usuario

class ConversasAdapter(
    private val onClick: (Conversa) -> Unit
) : Adapter<ConversasAdapter.ConversasViewHolder>(){

    private var listaConversas = emptyList<Conversa>()
    fun adicionarLista( lista: List<Conversa> ){
        listaConversas = lista
        notifyDataSetChanged()
    }

    inner class ConversasViewHolder(
        private val binding: ItemConversasBinding
    ): ViewHolder( binding.root ){

        fun bind( conversa: Conversa ){

            binding.textConversaNome.text = conversa.nome
            binding.textConversaMensagem.text = conversa.ultimaMensagem
            //Puxa a foto do banco de dados, desabilitado pois não tenho banco de dados
            /*Picasso.get()
                .load( conversa.foto )
                .into( binding.imageConversaFoto )*/

            //Evento de clique
            binding.clItemConversa.setOnClickListener {
                onClick( conversa )
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversasViewHolder {

        val inflater = LayoutInflater.from( parent.context )
        val itemView = ItemConversasBinding.inflate(
            inflater, parent, false
        )
        return ConversasViewHolder( itemView )

    }

    override fun onBindViewHolder(holder: ConversasViewHolder, position: Int) {

        val conversa = listaConversas[position]
        holder.bind( conversa )

    }

    override fun getItemCount(): Int {
        return listaConversas.size
    }

}