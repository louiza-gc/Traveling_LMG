package com.example.traveling.TravelShare.Inscription

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R

class CountryAdapter(
    private var countries: MutableList<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<CountryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCountry: TextView = itemView.findViewById(R.id.tvCountry)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_country, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = countries.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val country = countries[position]
        holder.tvCountry.text = country

        holder.itemView.setOnClickListener {
            onClick(country)
        }
    }

    fun updateList(newList: List<String>) {
        countries.clear()
        countries.addAll(newList)
        notifyDataSetChanged()
    }
}