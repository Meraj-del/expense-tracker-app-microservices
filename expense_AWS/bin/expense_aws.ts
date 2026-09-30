#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib/core';

import { ExpenseAwsStack } from '../lib/expense_aws-stack';
import { ExpenseTrackerServices } from '../lib/expense_service-stack';
import { ExpenseBackendServices } from '../lib/expense_backend_services-stack';

const app = new cdk.App();

const env = {
  account: process.env.CDK_DEFAULT_ACCOUNT,
  region: process.env.CDK_DEFAULT_REGION
}

const vpcStack = new ExpenseAwsStack(app, 'ExpenseAwsStack', {
  env,
});

const mysqlAndKafkaStack=  new ExpenseTrackerServices(app,`ExpenseTrackerServiceStack`,{
  env,
});

const backendServices = new ExpenseBackendServices(app,'ExpenseBackendService',{
  env
});
