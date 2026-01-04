import {ClientError, createChannel, createClientFactory, Metadata} from 'nice-grpc-web'
import {AdminServiceDefinition} from '../gen/admin-service'
import {PROXY_URL} from "../config/config.ts";
import {getTokenFromStorage} from "../hooks/use-login.ts";

console.log("Connecting to proxy: ", PROXY_URL);

const channel = createChannel(PROXY_URL)

const clientFactory = createClientFactory()
    .use((call, options) => {
        const authMetadata = Metadata({ 'authorization': 'Bearer ' + getTokenFromStorage() });

        return call.next(call.request, {
            ...options,
            metadata: { ...options.metadata, ...authMetadata }
        });
    });

export const adminClient = clientFactory.create(AdminServiceDefinition, channel)

export

const grpcErrorMessage = (message: string, error: any) => {
    if (error instanceof ClientError) return message + ": " + error.details;
    else return message + ": " + error.message;
}