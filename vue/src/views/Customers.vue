<template>
    <div>
        <div v-if="error">{{ error }}</div>
        <EntityList :columns="['id', 'number', 'prefix']" link-column="id" :entities="customers" />
    </div>
</template>

<script>
    import HSAdmin from "../hsadmin";
    import EntityList from "../components/EntityList";

    export default {
        name: "Customers",
        components: {EntityList},
        data () { return {
            error: false,
            customers: [],
        }},
        props: { hsadmin: HSAdmin },
        created () { this.fetch() },
        watch: { "$route": "fetch" },
        methods: {
            async fetch () {
                const res = await this.hsadmin.get("customers");
                this.customers = res.data;
            }
        },
    }
</script>
