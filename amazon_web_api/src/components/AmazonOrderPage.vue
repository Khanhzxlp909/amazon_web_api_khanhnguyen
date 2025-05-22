<template>
  <div class="p-6 max-w-7xl mx-auto">
    <h2 class="text-3xl font-bold mb-6 text-gray-800">📦 Amazon SP API Orders</h2>

    <div v-if="loading" class="text-blue-500 font-medium">Loading orders...</div>

    <!-- Orders Table -->
    <div v-if="orders.length" class="overflow-auto" style="height: 1000px; overflow-y: hidden;">
      <table class="table table-striped table-bordered table-lg" style="width: 1200px;">
        <thead style="background-color: #3498db; color: white;">
          <tr>
            <th v-for="(value, key) in orders[0].summary" :key="key" class="align-middle">
              {{ key }}
            </th>
            <th v-for="(value, key) in orders[0].items[0]" :key="'item-' + key" class="align-middle">
              Item {{ key }}
            </th>
            <th class="align-middle">
              State/Region
            </th>
            <th class="align-middle">
              Postal Code
            </th>
            <th class="align-middle">
              City
            </th>
            <th class="align-middle">
              Country Code
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.summary.AmazonOrderId">
            <td v-for="(value, key) in order.summary" :key="key" class="align-middle">
              <div class="text-sm text-gray-900">
                <span v-if="typeof value === 'object'">
                  {{ formatNestedObject(value) }}
                </span>
                <span v-else>
                  {{ value }}
                </span>
              </div>
            </td>
            <td v-for="(value, key) in order.items[0]" :key="'item-' + key" class="align-middle">
              <div class="text-sm text-gray-900">
                <span v-if="typeof value === 'object'">
                  {{ formatNestedObject(value) }}
                </span>
                <span v-else>
                  {{ value }}
                </span>
              </div>
            </td>
            <td class="align-middle">
              <div class="text-sm text-gray-900">
                {{ order.summary.ShippingAddress?.StateOrRegion || 'N/A' }}
              </div>
            </td>
            <td class="align-middle">
              <div class="text-sm text-gray-900">
                {{ order.summary.ShippingAddress?.PostalCode || 'N/A' }}
              </div>
            </td>
            <td class="align-middle">
              <div class="text-sm text-gray-900">
                {{ order.summary.ShippingAddress?.City || 'N/A' }}
              </div>
            </td>
            <td class="align-middle">
              <div class="text-sm text-gray-900">
                {{ order.summary.ShippingAddress?.CountryCode || 'N/A' }}
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- No orders -->
    <div v-else-if="!loading && fetched" class="text-center text-gray-500 mt-6">
      No orders found.
    </div>

    <!-- Error -->
    <div v-if="error" class="text-red-500 mt-4 font-medium">Error: {{ error }}</div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'

export default {
  setup() {
    const orders = ref([])
    const loading = ref(false)
    const fetched = ref(false)
    const error = ref(null)

    const fetchOrders = async () => {
      loading.value = true
      error.value = null
      fetched.value = false
      orders.value = []

      try {
        const response = await axios.get('http://localhost:8080/api/orders')
        orders.value = response.data || []
        fetched.value = true
        console.log(orders.value)
      } catch (err) {
        error.value = err.response?.data?.message || err.message
      } finally {
        loading.value = false
      }
    }

    const formatDate = (isoStr) => {
      const d = new Date(isoStr)
      return d.toLocaleString()
    }

    const getStatusClass = (status) => {
      switch (status) {
        case 'Shipped': return 'bg-green-100 text-green-800'
        case 'Unshipped': return 'bg-yellow-100 text-yellow-800'
        case 'Canceled': return 'bg-red-100 text-red-700'
        default: return 'bg-gray-100 text-gray-800'
      }
    }

    const formatNestedObject = (obj) => {
      if (typeof obj !== 'object' || obj === null) return obj
      return Object.entries(obj)
        .map(([key, value]) => `${key}: ${value}`)
        .join(', ')
    }

    onMounted(fetchOrders)

    return {
      orders,
      loading,
      fetched,
      error,
      formatDate,
      getStatusClass,
      formatNestedObject
    }
  }
}
</script>

<style scoped>
@import 'bootstrap/dist/css/bootstrap.min.css';

body {
  font-family: "Inter", sans-serif;
}
</style>
