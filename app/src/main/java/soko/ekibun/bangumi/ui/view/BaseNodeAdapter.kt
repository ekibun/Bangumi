package soko.ekibun.bangumi.ui.view

import android.view.View
import androidx.annotation.IdRes
import com.chad.library.adapter.base.entity.node.BaseExpandNode
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.entity.node.NodeFooterImp
import com.chad.library.adapter.base.viewholder.BaseViewHolder

abstract class BaseNodeAdapter : com.chad.library.adapter.base.BaseNodeAdapter() {
    /**
     * 处理点击事件
     */
    fun addOnClickListener(helper: BaseViewHolder, @IdRes id: Int) {
        helper.getView<View>(id).setOnClickListener {
            setOnItemChildClick(it, helper.layoutPosition)
        }
    }

    /**
     * 如果需要对某节点下的子节点进行数据操作，请使用[nodeRemoveData]！
     *
     * @param position Int 整个 data 的 index
     */
    override fun removeAt(position: Int) {
        val removeCount = removeNodesAt(position)
        notifyItemRangeRemoved(position + headerLayoutCount, removeCount)
        compatibilityDataSizeChanged(0)
    }

    /**
     * 如果需要对某节点下的子节点进行数据操作，请使用[nodeSetData]！
     * @param index Int
     * @param data BaseNode
     */
    override fun setData(index: Int, data: BaseNode) {
        // 先移除，再添加
        val removeCount = removeNodesAt(index)

        val newFlatData = flatData(arrayListOf(data))
        this.data.addAll(index, newFlatData)

        if (removeCount == newFlatData.size) {
            notifyItemRangeChanged(index + headerLayoutCount, removeCount)
        } else {
            notifyItemRangeRemoved(index + headerLayoutCount, removeCount)
            notifyItemRangeInserted(index + headerLayoutCount, newFlatData.size)

//        notifyItemRangeChanged(index + getHeaderLayoutCount(), max(removeCount, newFlatData.size)
        }
    }

    /**
     * 从数组中移除
     * @param position Int
     * @return Int 被移除的数量
     */
    private fun removeNodesAt(position: Int): Int {
        if (position >= data.size) {
            return 0
        }
        // 记录被移除的item数量
        var removeCount = 0

        // 先移除子项
        removeCount = removeChildAt(position)

        // 移除node自己
        this.data.removeAt(position)
        removeCount += 1

//        val node = this.data[position]
//        // 移除脚部
//        if (node is NodeFooterImp && node.footerNode != null) {
//            this.data.removeAt(position)
//            removeCount += 1
//        }
        return removeCount
    }

    private fun removeChildAt(position: Int): Int {
        if (position >= data.size) {
            return 0
        }
        // 记录被移除的item数量
        var removeCount = 0

        val node = this.data[position]
        // 移除子项
        if (!node.childNode.isNullOrEmpty()) {
            if (node is BaseExpandNode) {
                if (node.isExpanded) {
                    val items = flatData(node.childNode!!)
                    this.data.removeAll(items)
                    removeCount = items.size
                }
            } else {
                val items = flatData(node.childNode!!)
                this.data.removeAll(items)
                removeCount = items.size
            }
        }
        return removeCount
    }

    /**
     * 将输入的嵌套类型数组循环递归，在扁平化数据的同时，设置展开状态
     * @param list Collection<BaseNode>
     * @param isExpanded Boolean? 如果不需要改变状态，设置为null。true 为展开，false 为收起
     * @return MutableList<BaseNode>
     */
    private fun flatData(list: Collection<BaseNode>, isExpanded: Boolean? = null): MutableList<BaseNode> {
        val newList = ArrayList<BaseNode>()

        for (element in list) {
            newList.add(element)

            if (element is BaseExpandNode) {
                // 如果是展开状态 或者需要设置为展开状态
                if (isExpanded == true || element.isExpanded) {
                    val childNode = element.childNode
                    if (!childNode.isNullOrEmpty()) {
                        val items = flatData(childNode, isExpanded)
                        newList.addAll(items)
                    }
                }
                isExpanded?.let {
                    element.isExpanded = it
                }
            } else {
                val childNode = element.childNode
                if (!childNode.isNullOrEmpty()) {
                    val items = flatData(childNode, isExpanded)
                    newList.addAll(items)
                }
            }

            if (element is NodeFooterImp) {
                element.footerNode?.let {
                    newList.add(it)
                }
            }
        }
        return newList
    }
}