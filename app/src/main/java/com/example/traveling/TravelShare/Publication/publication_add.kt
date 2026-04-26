package com.example.traveling.TravelShare.Publication

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.traveling.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.io.File
import java.io.FileOutputStream

class PublicationAddFragment : Fragment(R.layout.fragment_publication_add) {

    private lateinit var viewModel: PublicationViewModel

    private lateinit var ivPreview: ImageView
    private lateinit var llNoMedia: LinearLayout
    private lateinit var rvThumbnails: RecyclerView
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var chipGroupTags: ChipGroup
    private lateinit var tvLocationLabel: TextView
    private lateinit var tvVisibilityLabel: TextView
    private lateinit var btnPublish: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var selectedMediaUri: Uri? = null
    private var galleryItems = mutableListOf<MediaItem>()
    private lateinit var thumbAdapter: ThumbnailAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // VIEWMODEL (comme ton profil)
        viewModel = ViewModelProvider(this)[PublicationViewModel::class.java]

        ivPreview = view.findViewById(R.id.ivPreview)
        llNoMedia = view.findViewById(R.id.llNoMedia)
        rvThumbnails = view.findViewById(R.id.rvThumbnails)
        etTitle = view.findViewById(R.id.etTitle)
        etDescription = view.findViewById(R.id.etDescription)
        chipGroupTags = view.findViewById(R.id.chipGroupTags)
        tvLocationLabel = view.findViewById(R.id.tvLocationLabel)
        tvVisibilityLabel = view.findViewById(R.id.tvVisibilityLabel)
        btnPublish = view.findViewById(R.id.btnPublish)
        progressBar = view.findViewById(R.id.progressBar)

        rvThumbnails.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        setupObservers()
        setupListeners()
        loadGallery()
    }

    // ───── LOCAL STORAGE IMAGE ─────
    private fun saveImageToInternalStorage(uri: Uri): String {

        val file = File(
            requireContext().filesDir,
            "post_${System.currentTimeMillis()}.jpg"
        )

        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)

        inputStream?.copyTo(outputStream)

        inputStream?.close()
        outputStream.close()

        return file.absolutePath
    }

    // ───── PUBLICATION ─────
    private fun publishPost() {

        val uri = selectedMediaUri ?: return

        val imagePath = saveImageToInternalStorage(uri)

        val title = etTitle.text.toString()
        val description = etDescription.text.toString()

        val tags = mutableListOf<String>()
        for (i in 0 until chipGroupTags.childCount) {
            val chip = chipGroupTags.getChildAt(i) as Chip
            if (chip.isChecked) tags.add(chip.text.toString())
        }

        viewModel.publishPost(
            imagePath = imagePath,
            title = title,
            description = description,
            locationName = tvLocationLabel.text.toString(),
            isPublic = true,
            tags = tags
        )
    }

    // ───── OBSERVERS ─────
    private fun setupObservers() {

        viewModel.isLoading.observe(viewLifecycleOwner) {
            progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }

        viewModel.publishSuccess.observe(viewLifecycleOwner) {
            if (it == true) {
                Toast.makeText(requireContext(), "Publié !", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) {
            it?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.aiTags.observe(viewLifecycleOwner) { tags ->
            chipGroupTags.removeAllViews()

            tags.forEach {
                val chip = Chip(requireContext())
                chip.text = it
                chip.isCheckable = true
                chip.isChecked = true
                chipGroupTags.addView(chip)
            }
        }
    }

    // ───── LISTENERS ─────
    private fun setupListeners() {

        btnPublish.setOnClickListener {
            publishPost()
        }
    }

    // ───── GALLERY (simplifié) ─────
    private fun loadGallery() {
        val list = mutableListOf<MediaItem>()

        val cursor = requireContext().contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                list.add(MediaItem(uri))
            }
        }

        galleryItems = list

        if (list.isNotEmpty()) {
            selectedMediaUri = list.first().uri
            Glide.with(this).load(list.first().uri).into(ivPreview)
        }

        thumbAdapter = ThumbnailAdapter(list) { index ->
            selectedMediaUri = list[index].uri
            Glide.with(this).load(list[index].uri).into(ivPreview)
        }

        rvThumbnails.adapter = thumbAdapter
    }
}