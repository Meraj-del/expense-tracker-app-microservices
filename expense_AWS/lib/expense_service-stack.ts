// ECS , Task Defination , Security Groups, NLB

import * as cdk from 'aws-cdk-lib';
import { Peer, Port, SecurityGroup, Subnet, Vpc } from 'aws-cdk-lib/aws-ec2';
import { AwsLogDriverMode, Cluster, ContainerImage, FargateService, FargateTaskDefinition, LogDriver } from 'aws-cdk-lib/aws-ecs';
import { NetworkLoadBalancer, NetworkTargetGroup, Protocol, TargetType } from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import { Construct } from 'constructs';

export class ExpenseTrackerServices extends cdk.Stack {

    constructor(scope: Construct, id: string, props?: cdk.StackProps) {

        super(scope, id, props);

        const vpcId = cdk.aws_ssm.StringParameter.valueFromLookup(this, 'VpcId');
        const vpc = Vpc.fromLookup(this, 'VpcImported', {
            vpcId: vpcId
        });

        const privateSubnet1 = Subnet.fromSubnetId(this, 'PrivateSubnet1', cdk.aws_ssm.StringParameter.valueFromLookup(this, 'PrivateSubnet-0'));
        const privateSubnet2 = Subnet.fromSubnetId(this, 'PrivateSubnet2', cdk.aws_ssm.StringParameter.valueFromLookup(this, 'PrivateSubnet-1'));

        const dbSecurityGroup = new SecurityGroup(this, 'DbSecurityGroup', {
            vpc,
            allowAllOutbound: true
        });

        dbSecurityGroup.addIngressRule(Peer.ipv4(vpc.vpcCidrBlock), Port.tcp(3306), "Allow MySQL Traffic"); //ingreessRule means rule for incoming traffic
        dbSecurityGroup.addIngressRule(Peer.ipv4(vpc.vpcCidrBlock), Port.tcp(9092), "Allow Kafka Traffic");

        const cluster = new Cluster(this, 'DatabaseKafkaCluster', {
            vpc,
            defaultCloudMapNamespace: {
                name: 'local'
            }
        });

        const nlb = new NetworkLoadBalancer(this, 'DatabaseNLB', {
            vpc,
            internetFacing: false,
            vpcSubnets: { subnets: [privateSubnet1, privateSubnet2] }
        });

        const mysqlTaskDefination = new FargateTaskDefinition(this, 'MySQLTaskDefination', {
            cpu: 256,
            memoryLimitMiB: 512,
        });
        mysqlTaskDefination.addContainer('MySQLContainer', {
            image: ContainerImage.fromRegistry('mysql:8.3.0'),
            environment: {
                MYSQL_ROOT_PASSWORD: 'Meraj786',
                MYSQL_USER: 'test',
                MYSQL_PASSWORD: 'Meraj786',
                MYSQL_ROOT_USER: 'root'
            },
            logging: LogDriver.awsLogs({
                streamPrefix: 'MySql',
                mode: AwsLogDriverMode.NON_BLOCKING,
                maxBufferSize: cdk.Size.mebibytes(25)
            }),
            portMappings: [{ containerPort: 3306 }]
        });

        const kafkaKraftTaskDefination = new FargateTaskDefinition(this, 'kafkaKraftTaskDefination', {
            cpu: 512,
            memoryLimitMiB: 2048,
        });
        kafkaKraftTaskDefination.addContainer('KafkaContainer', {
            image: ContainerImage.fromRegistry('apache/kafka:4.2.0'),
            environment: {
                KAFKA_NODE_ID: '1',
                KAFKA_PROCESS_ROLES: 'broker,controller',
                KAFKA_LISTENERS: 'PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093',
                KAFKA_ADVERTISED_LISTENERS: `PLAINTEXT://${nlb.loadBalancerDnsName}:9092`,
                KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER',
                KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT',
                KAFKA_CONTROLLER_QUORUM_VOTERS: '1@localhost:9093',
                KAFKA_LOG_DIRS: '/var/lib/kafka/data',
                KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: '3',
                KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: '1',
                KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: '1',
            },
            portMappings: [{ containerPort: 9092 },
            {
                containerPort: 9093
            }],
            logging: LogDriver.awsLogs({
                streamPrefix: 'Kafka',
                mode: AwsLogDriverMode.NON_BLOCKING,
                maxBufferSize: cdk.Size.mebibytes(25)
            }),
        });


        const mysqlService = new FargateService(this,'MySQLService',{
            cluster,
            taskDefinition: mysqlTaskDefination,
            desiredCount: 1,
            securityGroups: [dbSecurityGroup],
            vpcSubnets: {subnets: [privateSubnet1,privateSubnet2]}
        });

        const kafkaService = new FargateService(this,'KafkaService',{
            cluster,
            taskDefinition: kafkaKraftTaskDefination,
            desiredCount: 3,
            securityGroups: [dbSecurityGroup],
            vpcSubnets: {subnets: [privateSubnet1,privateSubnet2]}
        });

        const mysqlTargetGroup = new NetworkTargetGroup(this,'MySQLTargetGroup',{
            vpc,
            port: 3306,
            protocol: Protocol.TCP,
            targetType: TargetType.IP,
        });

        const kafkaTargetGroup = new NetworkTargetGroup(this,'KafkaTargetGroup',{
            vpc,
            port: 9092,
            protocol: Protocol.TCP,
            targetType: TargetType.IP,
        });

        mysqlTargetGroup.addTarget(mysqlService);
        kafkaTargetGroup.addTarget(kafkaService);

        nlb.addListener('MySQLListner',{
            port: 3306,
            protocol: Protocol.TCP,
            defaultTargetGroups: [mysqlTargetGroup]
        });

        nlb.addListener('KafkaListner',{
            port: 9092,
            protocol: Protocol.TCP,
            defaultTargetGroups: [kafkaTargetGroup]
        });

        new cdk.aws_ssm.StringParameter(this,`ExpenseTrackerServiceNLB`,{
            parameterName: `ExpenseTrackerServiceNLB`,
            stringValue: nlb.loadBalancerDnsName
        });
    }

}