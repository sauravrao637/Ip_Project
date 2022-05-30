/*****************************************************************************************
 * Copyright <2022> <Saurav Rao> <sauravrao637@gmail.com>                                *
 *                                                                                       *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this  *
 * software and associated documentation files (the "Software"), to deal in the Software *
 * without restriction, including without limitation the rights to use, copy, modify,    *
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to    *
 * permit persons to whom the Software is furnished to do so, subject to the following   *
 * conditions:                                                                           *
 *                                                                                       *
 * The above copyright notice and this permission notice shall be included in all copies *
 * or substantial portions of the Software.                                              *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,   *
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A         *
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT    *
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF  *
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE  *
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                                         *
 *****************************************************************************************/

package com.camo.ip_project.ui.savedreports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.camo.ip_project.database.local.model.UserHRV
import com.camo.ip_project.databinding.FragmentSavedHrvDataBinding
import com.camo.ip_project.ui.adapters.HrvDataRVAdapter
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Fragment for saved hrv analysis data
 */
@AndroidEntryPoint
class SavedHrvDataFragment : Fragment() {

    private val viewModel: SavedHrvDataViewModel by viewModels()
    private var _binding: FragmentSavedHrvDataBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HrvDataRVAdapter

    init {
        Timber.i("initialized")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedHrvDataBinding.inflate(inflater, container, false)
        adapter = HrvDataRVAdapter(arrayListOf())
        binding.rvHrvData.adapter = adapter
        binding.root.isEnabled = false
        val dividerItemDecoration = DividerItemDecoration(
            binding.rvHrvData.context,
            DividerItemDecoration.VERTICAL
        )
        binding.rvHrvData.addItemDecoration(dividerItemDecoration)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        val observer = Observer<List<UserHRV>>() {
            adapter.updateData(it)
        }
        viewModel.data.observe(viewLifecycleOwner, observer)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}