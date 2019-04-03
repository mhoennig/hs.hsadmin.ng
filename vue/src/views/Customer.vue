<template>
    <div>
        <div v-if="error">{{ error }}</div>
        This is customer {{ number }} with prefix {{ prefix }}.
        <h3>Contacts</h3>
        <EntityList :columns="['role','contactId','id','firstName','lastName']" :entities="contacts" />
    </div>
</template>

<script>
    import HSAdmin from "../hsadmin";
    import EntityList from "../components/EntityList";

    export default {
        name: "Customer",
        components: {EntityList},
        data () { return {
            error: false,
            number: undefined,
            prefix: undefined,
            contacts: [],
        }},
        props: { hsadmin: HSAdmin, id: Number },
        created () { this.fetch() },
        watch: { "$route": "fetch" },
        methods: {
            async fetch () {
                this.fetchCustomer();
                this.fetchContacts();
            },
            async fetchCustomer () {
                const res = await this.hsadmin.get(`customers/${this.id}`);
                this.number = res.data.number;
                this.prefix = res.data.prefix;
            },
            async fetchContacts () {
                const relationsResponse = await this.hsadmin.get('customer-contacts', { params: {
                    "customerId.equals": this.id,
                }});
                const relations = relationsResponse.data;
                // TODO: better param serializing
                const contactIdQuery = relations.map(relation => `id.in=${relation.contactId}`).join("&");
                const contactsResponse = await this.hsadmin.get(`contacts?${contactIdQuery}`);
                const contactsById = {};
                for (const contact of contactsResponse.data) {
                    contactsById[contact.id] = contact;
                }
                for (const relation of relations) {
                    Object.assign(relation, contactsById[relation.contactId]);
                }
                this.contacts = relations;
            },
        },
    }
</script>
