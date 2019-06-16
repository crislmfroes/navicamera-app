package com.crislmfroes.navicamera.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.crislmfroes.navicamera.R
import com.crislmfroes.navicamera.model.Marcador
import kotlinx.android.synthetic.main.list_item.view.*

class MarcadorAdapter(private val marcadores : List<Marcador>) : RecyclerView.Adapter<MarcadorAdapter.MarcadorHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarcadorHolder {
        val inflater = LayoutInflater.from(parent.context)
        val layout = inflater.inflate(R.layout.list_item, parent, false)
        return MarcadorHolder(layout)
    }

    override fun onBindViewHolder(holder: MarcadorHolder, position: Int) {
        holder.layout.codText.text = marcadores[position].cod.toString()
        holder.layout.nomeText.text = marcadores[position].nome
        holder.layout.distText.text = marcadores[position].distancia.toString()
        holder.layout.angleText.text = marcadores[position].rotacao.toString()
    }

    override fun getItemCount(): Int {
        return marcadores.size
    }

    inner class MarcadorHolder(val layout : View) : RecyclerView.ViewHolder(layout)
}