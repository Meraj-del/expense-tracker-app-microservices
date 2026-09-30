import * as cdk from 'aws-cdk-lib'
import { SecurityGroup, Subnet, Vpc } from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';
import * as assets from 'aws-cdk-lib/aws-ecr-assets';
import path from 'path';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import { RetentionDays } from 'aws-cdk-lib/aws-logs';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as servicediscovery from 'aws-cdk-lib/aws-servicediscovery';
import * as ec2 from 'aws-cdk-lib/aws-ec2';

export class ExpenseBackendServices extends cdk.Stack{

    constructor(scope: Construct,id: string, props?: cdk.StackProps){
        super(scope,id,props);

        const vpc = Vpc.fromLookup(this,'VpcImported',{
            vpcId: cdk.aws_ssm.StringParameter.valueFromLookup(this,'VpcId')
        });

        const privateSubnet1= Subnet.fromSubnetId(this,'PrivateSubnet1',cdk.aws_ssm.StringParameter.valueFromLookup(this,'PrivateSubnet-0'));
        const privateSubnet2= Subnet.fromSubnetId(this,'PrivateSubnet2',cdk.aws_ssm.StringParameter.valueFromLookup(this,'PrivateSubnet-1'));
        const publicSubnet1=Subnet.fromSubnetId(this,'PublicSubnet1',cdk.aws_ssm.StringParameter.valueFromLookup(this,'PublicSubnet-0'));
        const publicSubnet2=Subnet.fromSubnetId(this,'PublicSubnet2',cdk.aws_ssm.StringParameter.valueFromLookup(this,'PublicSubnet-1'));


        const nlbDnsName= cdk.aws_ssm.StringParameter.valueFromLookup(this,'ExpenseTrackerServiceNLB');
        
        const serviceSecurityGroup = new SecurityGroup(this,'BackendServiceSecurityGroup',{
            vpc,
            allowAllIpv6Outbound: true
        });

        const kongSecurityGroup= new SecurityGroup(this,'KongSecurityGroup',{
            vpc,
            allowAllOutbound: true,
            description: "Security Group for Kong"
        });

        serviceSecurityGroup.addIngressRule(
            kongSecurityGroup,
            ec2.Port.tcp(9898),
            'Allow traffic from kong to AuthService'
        );

        const namespace = new servicediscovery.PrivateDnsNamespace(this,'backendServiceDNSName',{
            name: 'public',
            vpc,
            description: "Namespace"
        })

        const authServiceImage = new assets.DockerImageAsset(this,'AuthService',{
            directory: path.join(__dirname, "..","AuthService1")
        });

        const kongServiceImage = new assets.DockerImageAsset(this,'KongServiceImage',{
            directory: path.join(__dirname,"..","Microservice1","Kong")
        });

        const userServiceImage = new assets.DockerImageAsset(this,'userServiceImage',{
            directory: path.join(__dirname,'..','Microservice1','userservice')
        });

        const expenseServiceImage = new assets.DockerImageAsset(this,'expenseServiceImage',{
            directory: path.join(__dirname,'..','Microservice1','ExpenseService')
        });

        const dsServiceImage = new assets.DockerImageAsset(this,'dsServiceImage',{
            directory: path.join(__dirname,'..','Microservice1','dsService')
        });

        const cluster = new ecs.Cluster(this,'ExpenseBackendCluster',{
            vpc: vpc
        });

        const authServiceTaskDef = new ecs.FargateTaskDefinition(this,'AuthServiceTaskDef',{
            memoryLimitMiB: 1024,
            cpu: 512
        });

        const kongServiceTaskDef = new ecs.FargateTaskDefinition(this,'KongServiceTaskDef',{
            memoryLimitMiB: 1024,
            cpu: 512
        });

        const userServiceTaskDef = new ecs.FargateTaskDefinition(this,'UserServiceTaskDef',{
            memoryLimitMiB: 1024,
            cpu: 512
        });

        const expenseServiceTaskDef = new ecs.FargateTaskDefinition(this,'ExpenseServiceTaskDef',{
            memoryLimitMiB: 512,
            cpu: 256
        });

        const dsServiceTaskDef = new ecs.FargateTaskDefinition(this,'dsServiceTaskDef',{
            memoryLimitMiB: 512,
            cpu: 256
        });


        authServiceTaskDef.addContainer('AuthServiceContainer',{
            image: ecs.ContainerImage.fromDockerImageAsset(authServiceImage),
            logging: ecs.LogDrivers.awsLogs({
                streamPrefix: "AuthService",
                logRetention: RetentionDays.ONE_WEEK
            }),
            portMappings: [{containerPort: 9898}],
            environment: {
                MYSQL_HOST: nlbDnsName,
                MYSQL_PORT: '3306',
                MYSQL_DB: 'authservice',
                MYSQL_USER: 'user',
                MYSQL_PASSWORD: 'Meraj786',
                KAFKA_HOST: nlbDnsName,
                KAFKA_PORT: '9092'
            }
        });

        kongServiceTaskDef.addContainer('KongServiceContainer',{
            image: ecs.ContainerImage.fromDockerImageAsset(kongServiceImage),
            logging: ecs.LogDriver.awsLogs({
                streamPrefix: "KongService",
                logRetention: RetentionDays.ONE_WEEK
            }),
            portMappings: [
                {containerPort: 8000}, //kong poxy port
                {containerPort: 8001} //Kong admin API Port
            ]
        });

        userServiceTaskDef.addContainer('UserServiceContainer',{
            image: ecs.ContainerImage.fromDockerImageAsset(userServiceImage),
            logging: ecs.LogDriver.awsLogs({
                streamPrefix: 'UserService',
                logRetention: RetentionDays.ONE_WEEK
            }),
            environment: {
                MYSQL_HOST: nlbDnsName,
                MYSQL_PORT: '3306',
                MYSQL_DB: 'userservice',
                MYSQL_USER: 'user',
                MYSQL_PASSWORD: 'Meraj786',
                KAFKA_HOST: nlbDnsName,
                KAFKA_PORT: '9092'
            },
            portMappings: [{containerPort: 9810}]
        });

        expenseServiceTaskDef.addContainer('ExpenseServiceContainer',{
            image: ecs.ContainerImage.fromDockerImageAsset(expenseServiceImage),
            logging: ecs.LogDriver.awsLogs({
                streamPrefix: "ExpenseService",
                logRetention: RetentionDays.ONE_WEEK
            }),
            environment: {
                MYSQL_HOST: nlbDnsName,
                MYSQL_PORT: '3306',
                MYSQL_DB: 'expenseservice',
                MYSQL_USER: 'user',
                MYSQL_PASSWORD: 'Meraj786',
                KAFKA_HOST: nlbDnsName,
                KAFKA_PORT: '9092'
            },
            portMappings: [{containerPort: 9820}]  
        });

        dsServiceTaskDef.addContainer('DsServiceContainer',{
            image: ecs.ContainerImage.fromDockerImageAsset(dsServiceImage),
            logging: ecs.LogDriver.awsLogs({
                streamPrefix: "DsService",
                logRetention: RetentionDays.ONE_WEEK,
            }),
            environment: {
                KAFKA_HOST: nlbDnsName,
                KAFKA_PORT: '9092',
                OPEN_API_KEY: '',
            },
            portMappings: [{containerPort: 9820}]
        });

        const authFargateService = new ecs.FargateService(this,'AuthService',{
            cluster: cluster,
            taskDefinition: authServiceTaskDef,
            desiredCount: 1,
            securityGroups: [serviceSecurityGroup],
            vpcSubnets: {subnets: [privateSubnet1,privateSubnet2]},
            assignPublicIp: false,
            enableExecuteCommand: true,
            cloudMapOptions: {
                name: 'auth-service',
                cloudMapNamespace: namespace,
                dnsRecordType: servicediscovery.DnsRecordType.A,
                dnsTtl: cdk.Duration.seconds(60)
            }
        });

        const userFargetService = new ecs.FargateService(this,'UserFargetService',{
            cluster: cluster,
            taskDefinition: userServiceTaskDef,
            desiredCount: 1,
            vpcSubnets: {subnets: [privateSubnet1,privateSubnet2]},
            securityGroups: [serviceSecurityGroup],
            assignPublicIp: false,
            enableExecuteCommand: true,
            cloudMapOptions: {
                name: 'user-service',
                cloudMapNamespace: namespace,
                dnsRecordType: servicediscovery.DnsRecordType.A,
                dnsTtl: cdk.Duration.seconds(60)
            }
        });

        const expenseFargetService = new ecs.FargateService(this,'ExpenseFargetService',{
            cluster: cluster,
            taskDefinition: expenseServiceTaskDef,
            desiredCount: 1,
            vpcSubnets: {subnets: [privateSubnet1,privateSubnet2]},
            securityGroups: [serviceSecurityGroup],
            assignPublicIp: false,
            enableExecuteCommand: true,
            cloudMapOptions: {
                name: 'expense-service',
                cloudMapNamespace: namespace,
                dnsRecordType: servicediscovery.DnsRecordType.A,
                dnsTtl: cdk.Duration.seconds(60)
            }
        });

        const dsFargetService = new ecs.FargateService(this,'DsFargetService',{
            cluster: cluster,
            taskDefinition: dsServiceTaskDef,
            desiredCount: 1,
            vpcSubnets: {subnets: [privateSubnet1,privateSubnet2]},
            securityGroups: [serviceSecurityGroup],
            assignPublicIp: false,
            enableExecuteCommand: true,
            cloudMapOptions: {
                name: 'ds-service',
                cloudMapNamespace: namespace,
                dnsRecordType: servicediscovery.DnsRecordType.A,
                dnsTtl: cdk.Duration.seconds(60)
            }
        });

        const kongFargetService = new ecs.FargateService(this,'KongFargetService',{
            cluster: cluster,
            taskDefinition: kongServiceTaskDef,
            desiredCount: 1, 
            vpcSubnets: {subnets: [publicSubnet1,publicSubnet2]},
            securityGroups: [kongSecurityGroup],
            assignPublicIp: true,
            cloudMapOptions: {
                name: 'kong',
                cloudMapNamespace: namespace,
                dnsRecordType: servicediscovery.DnsRecordType.A,
                dnsTtl: cdk.Duration.seconds(60)
            }
        });

        const authServiceAlb= new elbv2.ApplicationLoadBalancer(this,'AuthServiceALB',{
            vpc,
            internetFacing: false,
            vpcSubnets: {subnets: [privateSubnet1,privateSubnet2]}
        });

        const userServiceAlb = new elbv2.ApplicationLoadBalancer(this,'UserServiceALB',{
            vpc,
            internetFacing: false,
            vpcSubnets: {subnets: [privateSubnet1,privateSubnet2]}
        });

        const expenseServiceAlb = new elbv2.ApplicationLoadBalancer(this,'ExpenseServiceALB',{
            vpc,
            internetFacing: false,
            vpcSubnets: {subnets: [privateSubnet1,privateSubnet2]}
        });

        const dsServiceAlb = new elbv2.ApplicationLoadBalancer(this,'DsServiceALB',{
            vpc,
            internetFacing: false,
            vpcSubnets: {subnets: [privateSubnet1,privateSubnet2]}
        });

        const authServiceTargetGroup = new elbv2.ApplicationTargetGroup(this,'AuthServiceTargetGroup',{
            vpc,
            port: 9898,
            protocol: elbv2.ApplicationProtocol.HTTP,
            targetType: elbv2.TargetType.IP,
            healthCheck: {
                path: '/health',
                interval: cdk.Duration.seconds(60),
                timeout: cdk.Duration.seconds(30),
                healthyThresholdCount: 2,
                unhealthyThresholdCount: 3,
                healthyHttpCodes: '200-299'
            }
        });

        const userServiceTargetGroup = new elbv2.ApplicationTargetGroup(this,'UserServiceTargetGroup',{
            vpc,
            port: 9810,
            protocol: elbv2.ApplicationProtocol.HTTP,
            targetType: elbv2.TargetType.IP,
            healthCheck: {
                path: '/health',
                interval: cdk.Duration.seconds(60),
                timeout: cdk.Duration.seconds(30),
                healthyThresholdCount: 2,
                unhealthyThresholdCount: 3,
                healthyHttpCodes: '200-299'
            }
        });

        const expenseServiceTargetGroup = new elbv2.ApplicationTargetGroup(this,'ExpenseServiceTargetGroup',{
            vpc,
            port: 9820,
            protocol: elbv2.ApplicationProtocol.HTTP,
            targetType: elbv2.TargetType.IP,
            healthCheck: {
                path: '/health',
                interval: cdk.Duration.seconds(60),
                timeout: cdk.Duration.seconds(30),
                healthyThresholdCount: 2,
                unhealthyThresholdCount: 3,
                healthyHttpCodes: '200-299'
            }
        });

        const dsServiceTargetGroup = new elbv2.ApplicationTargetGroup(this,'DsServiceTargetGroup',{
            vpc,
            port: 8010,
            protocol: elbv2.ApplicationProtocol.HTTP,
            targetType: elbv2.TargetType.IP,
            healthCheck: {
                path: '/health',
                interval: cdk.Duration.seconds(60),
                timeout: cdk.Duration.seconds(30),
                healthyThresholdCount: 2,
                unhealthyThresholdCount: 3,
                healthyHttpCodes: '200-299'
            }
        });

        authServiceTargetGroup.addTarget(authFargateService);
        userServiceTargetGroup.addTarget(userFargetService);
        expenseServiceTargetGroup.addTarget(expenseFargetService)
        dsServiceTargetGroup.addTarget(dsFargetService);

        authServiceAlb.addListener('AuthServiceListener',{
            port: 80,
            defaultTargetGroups: [authServiceTargetGroup]
        });
        userServiceAlb.addListener('UserServiceAlbListener',{
            port: 80,
            defaultTargetGroups: [userServiceTargetGroup]
        });
        expenseServiceAlb.addListener('ExpenseServiceAlbListener',{
            port: 80,
            defaultTargetGroups: [expenseServiceTargetGroup]
        });
        dsServiceAlb.addListener('DsServiceAlbListener',{
            port: 80,
            defaultTargetGroups: [dsServiceTargetGroup]
        });

        kongSecurityGroup.addEgressRule(
            ec2.Peer.ipv4(vpc.vpcCidrBlock),
            ec2.Port.tcp(80),
            'Allow Kong to access Auth Service ALB'
        );

        // Add egress rules for direct Service ports
        kongSecurityGroup.addEgressRule(
            ec2.Peer.ipv4(vpc.vpcCidrBlock),
            ec2.Port.tcp(9898),
            'Allow Kong to access Auth Service directly'
        );

        kongSecurityGroup.addEgressRule(
            ec2.Peer.ipv4(vpc.vpcCidrBlock),
            ec2.Port.tcp(9810),
            'Allow Kong to access User Service directly'
        );

        kongSecurityGroup.addEgressRule(
            ec2.Peer.ipv4(vpc.vpcCidrBlock),
            ec2.Port.tcp(9820),
            'Allow Kong to access Expense Service directly'
        );

        kongSecurityGroup.addEgressRule(
            ec2.Peer.ipv4(vpc.vpcCidrBlock),
            ec2.Port.tcp(8010),
            'Allow Kong to access Ds Service directly'
        );

        authServiceAlb.connections.allowFrom(
            kongSecurityGroup,
            ec2.Port.tcp(80),
            'Allow traffic from kong to Auth Service ALB'
        );
        userServiceAlb.connections.allowFrom(
            kongSecurityGroup,
            ec2.Port.tcp(80),
            'Allow traffic from kong to User Service ALB'
        );
        expenseServiceAlb.connections.allowFrom(
            kongSecurityGroup,
            ec2.Port.tcp(80),
            'Allow traffic from kong to Expense Service ALB'
        );
        dsServiceAlb.connections.allowFrom(
            kongSecurityGroup,
            ec2.Port.tcp(80),
            'Allow traffic from kong to Ds Service ALB'
        );

        const kongALB = new elbv2.ApplicationLoadBalancer(this,'KongALB',{
            vpc,
            internetFacing: true,
            vpcSubnets: {subnets: [publicSubnet1,publicSubnet2]}
        });

        const kongTargetGroup= new elbv2.ApplicationTargetGroup(this,'KongTargetGroup',{
            vpc,
            port: 8000,
            protocol: elbv2.ApplicationProtocol.HTTP,
            targetType: elbv2.TargetType.IP,
            healthCheck: {
                path: '/health',
                interval: cdk.Duration.seconds(30),
                timeout: cdk.Duration.seconds(5),
                healthyThresholdCount: 2,
                unhealthyThresholdCount: 3,
                healthyHttpCodes: '200-299'
            }
        });

        kongTargetGroup.addTarget(kongFargetService);
        kongALB.addListener('KongListener',{
            port: 80,
            defaultTargetGroups: [kongTargetGroup]
        });

        new cdk.CfnOutput(this,'AuthServiceALBDNS',{
            value: authServiceAlb.loadBalancerDnsName,
            description: 'Auth Service ALB DNS Name',
        });

        




    }
}