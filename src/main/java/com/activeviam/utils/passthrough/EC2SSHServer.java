package com.activeviam.utils.passthrough;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.IpRange;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EC2SSHServer extends SSHServer {

	private static final Logger LOGGER = LoggerFactory.getLogger(EC2SSHServer.class);

	private AmazonEC2 aws;
	private String keyName;
	private Instance instance;

	public EC2SSHServer(Redirection... redirections) {
		super(redirections);
	}

	protected void deploy() {
		aws = AmazonEC2ClientBuilder.defaultClient();

		final Image ubuntuImage = getUbuntuImage();
		final RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
		runInstancesRequest.setImageId(ubuntuImage.getImageId());
		runInstancesRequest.setInstanceType(InstanceType.T2Micro);
		runInstancesRequest.setKeyName(this.getKeyName());
		runInstancesRequest.setSecurityGroupIds(this.generateSecurityGroups());
		runInstancesRequest.setMinCount(1);
		runInstancesRequest.setMaxCount(1);
		final RunInstancesResult runInstancesResult = aws.runInstances(runInstancesRequest);

		this.instance = runInstancesResult.getReservation().getInstances().get(0);
		this.user = "ubuntu";

		// Require to destroy that server on exit
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

		setupSSHServer();
	}

	private Collection<String> generateSecurityGroups() {
		ArrayList<String> groups = new ArrayList<>();
		groups.add("default");
		final CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest();
		final String groupName = "passthrough-" + this.id;
		createSecurityGroupRequest.setGroupName(groupName);
		createSecurityGroupRequest.setDescription("Exposer Security group, allow communication for redirected ports");
		aws.createSecurityGroup(createSecurityGroupRequest);
		groups.add(groupName);

		IpRange ip_range = new IpRange().withCidrIp("0.0.0.0/0");
		final int[] exposedPorts = getExposedPorts();

		ArrayList<IpPermission> permissions = new ArrayList<>(exposedPorts.length);
		for (int exposedPort : exposedPorts) {
			permissions.add(new IpPermission().withIpProtocol("tcp").withFromPort(exposedPort).withToPort(exposedPort).withIpv4Ranges(ip_range));
			permissions.add(new IpPermission().withIpProtocol("udp").withFromPort(exposedPort).withToPort(exposedPort).withIpv4Ranges(ip_range));
		}

		AuthorizeSecurityGroupIngressRequest auth_request = new AuthorizeSecurityGroupIngressRequest().withGroupName(groupName)
				.withIpPermissions((IpPermission[]) permissions.toArray(new IpPermission[permissions.size()]));

		aws.authorizeSecurityGroupIngress(auth_request);

		return groups;
	}

	private Image getUbuntuImage() {
		final DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest();
                // TRUE POSITIVE: https://github.com/activeviam/sin-passthrough-tool/issues/1
		describeImagesRequest.getFilters().add(new Filter("name").withValues("ubuntu/images/hvm-ssd/ubuntu-xenial-16.04-amd64-server-*"));
		final DescribeImagesResult describeImagesResult = aws.describeImages(describeImagesRequest);
		final List<Image> images = describeImagesResult.getImages();
		Image i = null;
		for (Image image : images) {
			if (i == null) {
				i = image;
			} else if (i.getCreationDate().compareTo(image.getCreationDate()) < 0) {
				i = image;
			}
		}
		return i;
	}

	public void shutdown() {
		final String instanceId = instance.getInstanceId();
		aws.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceId));
		Instance instance = null;

		do{
			if(instance != null) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException ignored) {}
			}
			instance = aws.describeInstances(new DescribeInstancesRequest().withInstanceIds(this.instance.getInstanceId())).getReservations()
					.get(0).getInstances().get(0);
		} while (instance.getState().getCode() != 48);

		aws.deleteKeyPair(new DeleteKeyPairRequest().withKeyName(this.keyName));
		aws.deleteSecurityGroup(new DeleteSecurityGroupRequest().withGroupName("passthrough-" + this.id));
	}

	public String getKeyName() {
		keyName = "passthrough-" + this.id;
		final ImportKeyPairRequest importKeyPairRequest = new ImportKeyPairRequest();
		importKeyPairRequest.withKeyName(keyName).withPublicKeyMaterial(this.publicKey);
		aws.importKeyPair(importKeyPairRequest);
		return keyName;
	}

	@Override public String getInstanceHost() {
		final Instance instance = aws.describeInstances(new DescribeInstancesRequest().withInstanceIds(this.instance.getInstanceId())).getReservations().get(0)
				.getInstances().get(0);
		if (instance.getPublicIpAddress() == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException ignored) {
			}
			return this.getInstanceHost();
		}
		return instance.getPublicIpAddress();
	}
}
