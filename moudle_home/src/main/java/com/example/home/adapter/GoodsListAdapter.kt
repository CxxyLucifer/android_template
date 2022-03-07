package com.example.home.adapter

import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import coil.ImageLoader
import coil.load
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.example.home.R
import com.example.home.GoodsBean

class GoodsListAdapter(layoutResId: Int, data: MutableList<GoodsBean>) : BaseQuickAdapter<GoodsBean, BaseViewHolder>(layoutResId, data) {
    private lateinit var imageLoader: ImageLoader

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        imageLoader = Coil.imageLoader(context)
    }

    override fun convert(holder: BaseViewHolder, item: GoodsBean) {
        holder.setText(R.id.tv_title, item.name)
              .setText(R.id.tv_price, item.price)

        holder.getView<ImageView>(R.id.iv_img).load(item.url, imageLoader ) {
            crossfade(true)
            placeholder(R.drawable.default_img)
            error(R.drawable.default_img)
        }
    }
}