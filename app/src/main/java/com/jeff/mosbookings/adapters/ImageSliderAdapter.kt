package com.jeff.mosbookings.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jeff.mosbookings.R

class ImageSliderAdapter(private val images: List<String>) : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.sliderImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_slider, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = images[position]
        println("Loading image at position $position: $imageUrl")
        
        val validImageUrl = imageUrl.takeIf { it.isNotBlank() && it != "placeholder" }
        
        if (validImageUrl != null) {
            println("Loading valid URL: $validImageUrl")
            // Load image from URL
            Glide.with(holder.imageView.context)
                .load(validImageUrl)
                .placeholder(R.drawable.placeholder_room)
                .error(R.drawable.placeholder_room)
                .centerCrop()
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        println("Failed to load image: $validImageUrl, Error: ${e?.message}")
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        println("Successfully loaded image: $validImageUrl")
                        return false
                    }
                })
                .into(holder.imageView)
        } else {
            println("Loading placeholder drawable")
            // Load placeholder drawable
            holder.imageView.setImageResource(R.drawable.placeholder_room)
        }
    }

    override fun getItemCount(): Int = images.size
} 